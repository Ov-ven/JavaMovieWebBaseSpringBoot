package com.software.movie.entity.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 支付请求DTO，封装发起支付所需的参数。
 * 用于向第三方支付平台（如支付宝、微信）发起支付请求。
 */
@Data
public class PaymentRequest {
    /** 订单编号 */
    private String orderNumber;
    /** 支付金额 */
    private BigDecimal amount;
    /** 商品名称 */
    private String productName;
    /** 商品描述 */
    private String description;
}
