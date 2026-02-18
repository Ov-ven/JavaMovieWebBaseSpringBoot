package com.software.movie.service;

import com.alipay.api.AlipayApiException;
import com.software.movie.entity.dto.PaymentRequest;
import javax.servlet.http.HttpServletRequest;

public interface AlipayService {
    String createPayment(PaymentRequest request) throws AlipayApiException;
    boolean verifyNotification(HttpServletRequest request);
    void handlePaymentNotification(HttpServletRequest request);
}