package com.software.movie.service;

import com.alipay.api.AlipayApiException;
import com.software.movie.entity.dto.PaymentRequest;

import javax.servlet.http.HttpServletRequest;

/**
 * 支付宝支付服务接口。
 * 提供电脑网站支付的创建、回调验签及异步通知处理能力。
 */
public interface AlipayService {

    /**
     * 创建电脑网站支付，返回支付页面 HTML
     *
     * @param request 支付请求参数，包含订单号、金额、商品名称等
     * @return 支付宝收银台页面的 HTML 字符串（自动提交表单）
     * @throws AlipayApiException 调用支付宝接口失败时抛出
     */
    String createPayment(PaymentRequest request) throws AlipayApiException;

    /**
     * 验证支付宝回调签名
     *
     * @param request 支付宝回调的 HTTP 请求
     * @return 验签是否通过
     */
    boolean verifyNotification(HttpServletRequest request);

    /**
     * 处理支付宝异步回调通知（验签 + 更新订单 + 升级VIP）
     *
     * @param request 支付宝回调的 HTTP 请求
     * @return true=处理成功, false=处理失败
     */
    boolean handlePaymentNotification(HttpServletRequest request);

    /**
     * 主动查询支付宝订单状态（用于回调丢失时手动同步）
     *
     * @param orderNo 我方订单号
     * @return 交易状态字符串（TRADE_SUCCESS 等），查询失败返回 null
     */
    String queryTradeStatus(String orderNo);
}
