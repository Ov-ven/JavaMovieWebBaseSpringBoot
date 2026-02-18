package com.software.movie.controller;

import com.alipay.api.AlipayApiException;
import com.software.movie.entity.dto.PaymentRequest;
import com.software.movie.service.AlipayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;

@Controller
@RequestMapping("/payment")
public class PaymentController {

    private final AlipayService alipayService;

    @Autowired
    public PaymentController(AlipayService alipayService) {
        this.alipayService = alipayService;
    }

    @PostMapping("/create")
    @ResponseBody
    public String createPayment() {
        try {
            PaymentRequest request = new PaymentRequest();
            request.setOrderNumber("ORD" + System.currentTimeMillis());
            request.setAmount(new BigDecimal("49.90"));
            request.setProductName("季度VIP会员");
            request.setDescription("享受三个月VIP会员特权");

            // 返回支付宝支付页面HTML
            return alipayService.createPayment(request);
        } catch (AlipayApiException e) {
            return "<h1>支付创建失败: " + e.getMessage() + "</h1>";
        }
    }

    // 支付宝回调通知接口
    @RequestMapping("/notify")
    @ResponseBody
    public String handlePaymentNotification() {
        alipayService.handlePaymentNotification(null);
        return "success"; // 返回success告诉支付宝已收到通知
    }
}