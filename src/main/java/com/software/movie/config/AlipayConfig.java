package com.software.movie.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlipayConfig {

    @Value("${alipay.appId}")
    private String appId;

    @Value("${alipay.appPrivateKey}")
    private String appPrivateKey;

    @Value("${alipay.alipayPublicKey}")
    private String alipayPublicKey;

    @Value("${alipay.gateway:https://openapi-sandbox.dl.alipaydev.com/gateway.do}")
    private String gateway;

    @Value("${alipay.notifyUrl}")
    private String notifyUrl;

    @Value("${alipay.returnUrl:}") // 注意这里如果设置为/,也要确保正确配置
    private String returnUrl;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppPrivateKey() {
        return appPrivateKey;
    }

    public void setAppPrivateKey(String appPrivateKey) {
        this.appPrivateKey = appPrivateKey;
    }

    public String getAlipayPublicKey() {
        return alipayPublicKey;
    }

    public void setAlipayPublicKey(String alipayPublicKey) {
        this.alipayPublicKey = alipayPublicKey;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    @Bean
    public AlipayClient alipayClient() {
        // !!! 关键修改：修正DefaultAlipayClient构造函数参数顺序和补充signType !!!
        return new DefaultAlipayClient(
                gateway,        // 支付宝网关
                appId,          // 应用APPID
                appPrivateKey,  // 应用私钥
                "json",         // format，通常是 "json" 或 "xml"，不是 "RSA2"
                "UTF-8",        // charset
                alipayPublicKey,// 支付宝公钥
                "RSA2"          // signType，签名类型
        );
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public String getGateway() {
        return gateway;
    }
}
