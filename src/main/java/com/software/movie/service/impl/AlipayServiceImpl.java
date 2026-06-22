package com.software.movie.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.software.movie.config.AlipayConfig;
import com.software.movie.entity.Order;
import com.software.movie.entity.User;
import com.software.movie.entity.dto.PaymentRequest;
import com.software.movie.mapper.OrderMapper;
import com.software.movie.service.AlipayService;
import com.software.movie.service.OrderService;
import com.software.movie.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝支付服务实现类。
 * 基于支付宝电脑网站支付（alipay.trade.page.pay）实现支付创建、
 * 异步回调验签、订单状态更新及VIP自动升级。
 */
@Service
public class AlipayServiceImpl implements AlipayService {

    private static final Logger log = LoggerFactory.getLogger(AlipayServiceImpl.class);

    private AlipayClient alipayClient;
    private final AlipayConfig alipayConfig;
    private final OrderService orderService;
    private final UserService userService;
    private final OrderMapper orderMapper;

    @Autowired
    public AlipayServiceImpl(@Autowired(required = false) AlipayClient alipayClient, AlipayConfig alipayConfig,
                             OrderService orderService, UserService userService,
                             com.software.movie.mapper.OrderMapper orderMapper) {
        this.alipayClient = alipayClient;
        this.alipayConfig = alipayConfig;
        this.orderService = orderService;
        this.userService = userService;
        this.orderMapper = orderMapper;
    }

    /**
     * 创建电脑网站支付
     * 按照支付宝官方文档：alipay.trade.page.pay
     *
     * @param request 支付请求参数
     * @return 支付宝收银台页面的 HTML 字符串
     * @throws AlipayApiException 调用支付宝接口失败时抛出
     */
    @Override
    public String createPayment(PaymentRequest request) throws AlipayApiException {
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();

        // 设置异步通知地址和同步跳转地址
        alipayRequest.setNotifyUrl(alipayConfig.getNotifyUrl());
        alipayRequest.setReturnUrl(alipayConfig.getReturnUrl());

        // 构建业务参数
        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        model.setOutTradeNo(request.getOrderNumber());                    // 商户订单号
        model.setTotalAmount(request.getAmount().toPlainString());        // 订单金额（字符串，保留两位小数）
        model.setSubject(request.getProductName());                       // 订单标题
        model.setBody(request.getDescription());                          // 订单描述
        model.setProductCode("FAST_INSTANT_TRADE_PAY");                   // 产品码（必填）
        model.setTimeoutExpress("30m");                                   // 超时时间

        alipayRequest.setBizModel(model);

        // 执行请求，获取支付页面 HTML
        AlipayTradePagePayResponse response = alipayClient.pageExecute(alipayRequest, "POST");

        log.info("支付宝响应 - success: {}, msg: {}, subCode: {}, subMsg: {}",
                response.isSuccess(), response.getMsg(), response.getSubCode(), response.getSubMsg());

        if (!response.isSuccess()) {
            throw new AlipayApiException("支付宝创建支付失败: " + response.getSubMsg());
        }

        // 返回支付宝收银台页面的 HTML（自动提交表单，跳转到支付宝收银台）
        return response.getBody();
    }

    /**
     * 处理支付宝异步回调通知（按支付宝官方规范实现）
     * 参考文档：https://opendocs.alipay.com/open/270/105902
     *
     * @param request 支付宝回调的 HTTP 请求
     * @return true=处理成功, false=处理失败
     */
    @Override
    @Transactional
    public boolean handlePaymentNotification(HttpServletRequest request) {
        // 1. 提取所有回调参数
        Map<String, String> params = extractParams(request);

        // 2. 验签（按照支付宝官方规范，必须先验签再处理业务）
        boolean signVerified;
        try {
            signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    alipayConfig.getAlipayPublicKey(),
                    "UTF-8",
                    "RSA2"
            );
        } catch (AlipayApiException e) {
            log.error("支付宝验签异常", e);
            return false;
        }

        if (!signVerified) {
            log.warn("支付宝回调验签失败");
            return false;
        }

        // 3. 提取关键参数
        String outTradeNo = params.get("out_trade_no");     // 我方订单号
        String tradeNo = params.get("trade_no");             // 支付宝交易号
        String tradeStatus = params.get("trade_status");     // 交易状态
        String totalAmount = params.get("total_amount");     // 订单金额

        log.info("支付宝回调 - 订单号: {}, 支付宝交易号: {}, 状态: {}, 金额: {}",
                outTradeNo, tradeNo, tradeStatus, totalAmount);

        // 4. 验证订单是否存在
        Order order = orderService.getByOrderNo(outTradeNo);
        if (order == null) {
            log.warn("订单不存在: {}", outTradeNo);
            return false;
        }

        // 5. 验证金额是否一致（防篡改）
        // 混合支付时，支付宝金额 = 订单总额 - 已用余额支付
        double balancePaid = order.getBalancePaid() == null ? 0 : order.getBalancePaid();
        double expectedAlipayAmount = order.getAmount() - balancePaid;
        if (!totalAmount.equals(String.format("%.2f", expectedAlipayAmount))) {
            log.warn("支付宝金额不匹配 - 预期: {}, 回调: {}", expectedAlipayAmount, totalAmount);
            return false;
        }

        // 6. 处理交易状态
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            // 幂等判断：如果订单已支付，直接返回成功
            if (order.getStatus() == 1) {
                log.info("订单已处理过，跳过: {}", outTradeNo);
                return true;
            }

            // 更新订单状态为已支付
            boolean updated = orderService.payOrder(outTradeNo, 1, tradeNo);
            if (!updated) {
                log.error("更新订单状态失败: {}", outTradeNo);
                return false;
            }

            // 仅 VIP 购买订单(orderType=2)才升级 VIP，单片购买(orderType=1)不升级
            if (order.getOrderType() != null && order.getOrderType() == 2) {
                int months = determineMonths(order.getAmount());
                User user = userService.getById(order.getUserId());
                if (user != null) {
                    userService.upgradeToVip(user.getId(), months);
                    log.info("用户 {} 升级VIP {} 个月成功", user.getId(), months);
                }
            } else {
                log.info("订单 {} 为单片购买，不升级VIP", outTradeNo);
            }

            log.info("订单 {} 支付处理完成", outTradeNo);
            return true;
        }

        // 7. 交易关闭（用户取消/超时）→ 退回已扣余额
        if ("TRADE_CLOSED".equals(tradeStatus)) {
            log.info("交易关闭，处理退款：订单号 {}", outTradeNo);
            if (order.getBalancePaid() != null && order.getBalancePaid() > 0) {
                userService.refundBalance(order.getUserId(), order.getBalancePaid());
                log.info("已退回余额 ¥{} 给用户 {}", order.getBalancePaid(), order.getUserId());
                order.setBalancePaid(0.0);
                orderMapper.updateById(order);
            }
            return true;
        }

        log.info("交易状态非终态，忽略: {}", tradeStatus);
        return true;
    }

    /**
     * 根据金额判断VIP时长
     *
     * @param amount 订单金额
     * @return VIP月数（159及以上=12个月，49.9及以上=3个月，其余=1个月）
     */
    private int determineMonths(Double amount) {
        if (amount == null) return 1;
        if (amount >= 159.0) return 12;   // 年度
        if (amount >= 49.9) return 3;     // 季度
        return 1;                          // 月度
    }

    /**
     * 主动查询支付宝订单状态（alipay.trade.query）
     */
    @Override
    public String queryTradeStatus(String orderNo) {
        try {
            com.alipay.api.request.AlipayTradeQueryRequest request = new com.alipay.api.request.AlipayTradeQueryRequest();
            request.setBizContent("{\"out_trade_no\":\"" + orderNo + "\"}");
            com.alipay.api.response.AlipayTradeQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                return response.getTradeStatus();
            }
            log.warn("支付宝查询失败: {}", response.getSubMsg());
            return null;
        } catch (Exception e) {
            log.error("支付宝查询异常", e);
            return null;
        }
    }

    /**
     * 从 HttpServletRequest 中提取所有参数
     *
     * @param request HTTP 请求
     * @return 参数名-参数值映射
     */
    private Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (String name : requestParams.keySet()) {
            String[] values = requestParams.get(name);
            StringBuilder valueStr = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                valueStr.append(i == values.length - 1 ? values[i] : values[i] + ",");
            }
            params.put(name, valueStr.toString());
        }
        return params;
    }

    /**
     * 验证支付宝回调签名
     *
     * @param request 支付宝回调的 HTTP 请求
     * @return 验签是否通过
     */
    @Override
    public boolean verifyNotification(HttpServletRequest request) {
        Map<String, String> params = extractParams(request);
        try {
            return AlipaySignature.rsaCheckV1(
                    params,
                    alipayConfig.getAlipayPublicKey(),
                    "UTF-8",
                    "RSA2"
            );
        } catch (AlipayApiException e) {
            log.error("验签异常", e);
            return false;
        }
    }
}
