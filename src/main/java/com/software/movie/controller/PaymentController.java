package com.software.movie.controller;

import com.alipay.api.AlipayApiException;
import com.software.movie.common.Result;
import com.software.movie.entity.Order;
import com.software.movie.entity.User;
import com.software.movie.entity.dto.PaymentRequest;
import com.software.movie.service.AlipayService;
import com.software.movie.service.OrderService;
import com.software.movie.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * 支付管理控制器
 * <p>
 * 处理 VIP 会员购买的支付流程，包括创建支付宝支付订单、
 * 直接购买（测试/演示用）以及支付宝异步回调通知处理。
 * </p>
 */
@Controller
@RequestMapping("/payment")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final AlipayService alipayService;
    private final OrderService orderService;
    private final UserService userService;

    @Autowired
    public PaymentController(AlipayService alipayService, OrderService orderService, UserService userService) {
        this.alipayService = alipayService;
        this.orderService = orderService;
        this.userService = userService;
    }

    /**
     * 创建支付（VIP购买）
     * 接收前端传来的套餐信息，先创建订单，再调用支付宝
     */
    @PostMapping("/create")
    @ResponseBody
    public String createPayment(@RequestParam String planType, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "<h1>请先登录</h1>";
        }

        // 根据套餐类型确定金额和商品名
        double amount;
        String productName;
        switch (planType) {
            case "monthly":
                amount = 19.90;
                productName = "月度VIP会员";
                break;
            case "yearly":
                amount = 159.00;
                productName = "年度VIP会员";
                break;
            default: // quarterly
                amount = 49.90;
                productName = "季度VIP会员";
                break;
        }

        // 1. 先在数据库创建订单（orderType=2, status=0）
        Order order = orderService.createVipOrder(user.getId(), amount);

        // 2. 构建支付请求
        PaymentRequest request = new PaymentRequest();
        request.setOrderNumber(order.getOrderNo());
        request.setAmount(new java.math.BigDecimal(String.valueOf(amount)));
        request.setProductName(productName);
        request.setDescription(productName + " - 电影平台");

        // 3. 调用支付宝，返回支付页面 HTML
        try {
            return alipayService.createPayment(request);
        } catch (AlipayApiException e) {
            log.error("创建支付宝支付失败", e);
            return "<h1>支付创建失败: " + e.getMessage() + "</h1>";
        }
    }

    /**
     * 直接购买（跳过支付宝，用于测试/演示）
     */
    @PostMapping("/directBuy")
    @ResponseBody
    public Result directBuy(@RequestParam String planType, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error("请先登录");
        }

        double amount;
        int months;
        switch (planType) {
            case "monthly":
                amount = 19.90; months = 1; break;
            case "yearly":
                amount = 159.00; months = 12; break;
            default:
                amount = 49.90; months = 3; break;
        }

        // 创建订单并直接标记已支付
        Order order = orderService.createVipOrder(user.getId(), amount);
        orderService.payOrder(order.getOrderNo(), 0, "DIRECT_BUY");

        // 升级VIP
        userService.upgradeToVip(user.getId(), months);

        // 刷新 session 中的用户信息
        session.setAttribute("user", userService.getByUsername(user.getUsername()));

        log.info("用户 {} 直接购买VIP {} 个月", user.getId(), months);
        return Result.success("开通成功");
    }

    /**
     * 支付宝异步回调通知（按支付宝官方规范实现）
     */
    @PostMapping("/notify")
    @ResponseBody
    public String handlePaymentNotification(HttpServletRequest request) {
        log.info("收到支付宝异步回调通知");
        try {
            boolean success = alipayService.handlePaymentNotification(request);
            return success ? "success" : "fail";
        } catch (Exception e) {
            log.error("处理支付宝回调异常", e);
            return "fail";
        }
    }
}
