package com.software.movie.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 支付宝支付配置类。
 * <p>从 application.properties 读取支付宝相关配置（appId、密钥、网关地址等），
 * 并注册 {@link AlipayClient} Bean 供支付业务使用。</p>
 *
 * <p>支持的配置项：</p>
 * <ul>
 *   <li>{@code alipay.appId} - 应用ID</li>
 *   <li>{@code alipay.appPrivateKey} - 应用私钥</li>
 *   <li>{@code alipay.alipayPublicKey} - 支付宝公钥</li>
 *   <li>{@code alipay.gateway} - 网关地址（默认沙箱环境）</li>
 *   <li>{@code alipay.notifyUrl} - 异步通知回调地址</li>
 *   <li>{@code alipay.returnUrl} - 同步跳转地址</li>
 * </ul>
 */
@Configuration
public class AlipayConfig {

    private static final Logger log = LoggerFactory.getLogger(AlipayConfig.class);

    /**
     * 应用ID
     */
    @Value("${alipay.appId:}")
    private String appId;

    /**
     * 应用私钥
     */
    @Value("${alipay.appPrivateKey:}")
    private String appPrivateKey;

    /**
     * 支付宝公钥
     */
    @Value("${alipay.alipayPublicKey:}")
    private String alipayPublicKey;

    /**
     * 支付宝网关地址（默认沙箱环境）
     */
    @Value("${alipay.gateway:https://openapi-sandbox.dl.alipaydev.com/gateway.do}")
    private String gateway;

    /**
     * 异步通知回调URL
     */
    @Value("${alipay.notifyUrl}")
    private String notifyUrl;

    /**
     * 同步跳转URL
     */
    @Value("${alipay.returnUrl:}") // 注意这里如果设置为/,也要确保正确配置
    private String returnUrl;

    /**
     * 获取应用ID。
     *
     * @return 应用ID
     */
    public String getAppId() {
        return appId;
    }

    /**
     * 设置应用ID。
     *
     * @param appId 应用ID
     */
    public void setAppId(String appId) {
        this.appId = appId;
    }

    /**
     * 获取应用私钥。
     *
     * @return 应用私钥
     */
    public String getAppPrivateKey() {
        return appPrivateKey;
    }

    /**
     * 设置应用私钥。
     *
     * @param appPrivateKey 应用私钥
     */
    public void setAppPrivateKey(String appPrivateKey) {
        this.appPrivateKey = appPrivateKey;
    }

    /**
     * 获取支付宝公钥。
     *
     * @return 支付宝公钥
     */
    public String getAlipayPublicKey() {
        return alipayPublicKey;
    }

    /**
     * 设置支付宝公钥。
     *
     * @param alipayPublicKey 支付宝公钥
     */
    public void setAlipayPublicKey(String alipayPublicKey) {
        this.alipayPublicKey = alipayPublicKey;
    }

    /**
     * 设置网关地址。
     *
     * @param gateway 网关地址
     */
    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    /**
     * 设置异步通知回调URL。
     *
     * @param notifyUrl 异步通知回调URL
     */
    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    /**
     * 设置同步跳转URL。
     *
     * @param returnUrl 同步跳转URL
     */
    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    /**
     * 创建支付宝客户端 Bean。
     * <p>使用 RSA2 签名方式，JSON 格式，UTF-8 编码。</p>
     *
     * @return 配置好的 {@link AlipayClient} 实例
     */
    @Bean
    public AlipayClient alipayClient() {
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appPrivateKey) || !StringUtils.hasText(alipayPublicKey)) {
            log.warn("支付宝配置不完整（Alipay_AppId / Alipay_AppPrivateKey / Alipay_AlipayPublicKey），跳过 AlipayClient 创建");
            return null;
        }
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

    /**
     * 获取异步通知回调URL。
     *
     * @return 异步通知回调URL
     */
    public String getNotifyUrl() {
        return notifyUrl;
    }

    /**
     * 获取同步跳转URL。
     *
     * @return 同步跳转URL
     */
    public String getReturnUrl() {
        return returnUrl;
    }

    /**
     * 获取网关地址。
     *
     * @return 网关地址
     */
    public String getGateway() {
        return gateway;
    }
}
