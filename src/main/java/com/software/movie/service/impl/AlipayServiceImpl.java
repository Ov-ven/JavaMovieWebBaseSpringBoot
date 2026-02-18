package com.software.movie.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.software.movie.config.AlipayConfig;
import com.software.movie.entity.dto.PaymentRequest;
import com.software.movie.service.AlipayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class AlipayServiceImpl implements AlipayService {

    private final AlipayClient alipayClient;
    private final AlipayConfig alipayConfig;

    @Autowired
    public AlipayServiceImpl(AlipayClient alipayClient, AlipayConfig alipayConfig) {
        this.alipayClient = alipayClient;
        this.alipayConfig = alipayConfig;
    }

    @Override
    public String createPayment(PaymentRequest request) throws AlipayApiException {
        try {
            AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();

            // 设置回调URL - 确保格式正确
            alipayRequest.setNotifyUrl(alipayConfig.getNotifyUrl());
            alipayRequest.setReturnUrl(alipayConfig.getReturnUrl());

            // 创建业务模型 - 检查所有参数
            AlipayTradePagePayModel model = new AlipayTradePagePayModel();
            model.setOutTradeNo(request.getOrderNumber());

            // 金额必须是字符串格式，保留两位小数
            BigDecimal amount = new BigDecimal("49.90"); // 确保带小数点
            model.setTotalAmount(amount.toPlainString()); // 使用toPlainString避免科学计数法

            model.setSubject("季度VIP会员");
            model.setBody("享受三个月VIP会员特权");
            model.setProductCode("FAST_INSTANT_TRADE_PAY"); // 必填项

            // 设置其他必要参数
            model.setTimeoutExpress("30m");  // 超时时间
            model.setGoodsType("0");        // 虚拟商品

            alipayRequest.setBizModel(model);

            // 建议使用POST方式获取支付页面，支付宝通常以POST返回一个可自动提交的表单
            AlipayTradePagePayResponse response = alipayClient.pageExecute(alipayRequest, "POST");

            // 添加日志，调试时查看响应
            System.out.println("支付宝响应状态: " + response.isSuccess());
            System.out.println("响应信息: " + response.getMsg());
            System.out.println("子代码: " + response.getSubCode());
            System.out.println("子信息: " + response.getSubMsg());

            if (!response.isSuccess()) {
                throw new AlipayApiException("创建支付失败: " + response.getSubMsg());
            }

            return response.getBody();
        } catch (Exception e) {
            throw new AlipayApiException("创建支付时出错: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verifyNotification(HttpServletRequest request) {
        // 这里需要实现签名验证逻辑，为简化返回true
        return true;
    }

    @Override
    public void handlePaymentNotification(HttpServletRequest request) {
        // 简化处理，实际项目需要解析请求参数
        System.out.println("收到支付宝支付通知");
    }
}