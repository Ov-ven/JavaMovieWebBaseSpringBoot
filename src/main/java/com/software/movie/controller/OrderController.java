package com.software.movie.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.software.movie.common.Result;
import com.software.movie.common.UserContext;
import com.software.movie.entity.Movie;
import com.software.movie.entity.Order;
import com.software.movie.entity.User;
import com.software.movie.mapper.MovieMapper;
import com.software.movie.mapper.OrderMapper;
import com.software.movie.service.AlipayService;
import com.software.movie.service.OrderService;
import com.software.movie.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单管理控制器
 * <p>
 * 提供订单相关的 RESTful API，包括单片购买下单等功能。
 * </p>
 */
@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @Autowired
    private AlipayService alipayService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private MovieMapper movieMapper;

    /**
     * 获取当前用户的订单列表
     *
     * @return 订单列表（含电影名称）
     */
    @GetMapping("/my")
    public Result getMyOrders() {
        User user = UserContext.getUser();
        if (user == null) return Result.error("未登录");

        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", user.getId()).orderByDesc("create_time");
        List<Order> orders = orderMapper.selectList(wrapper);

        // 批量查询电影名称
        Set<Long> movieIds = orders.stream()
                .map(Order::getMovieId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> movieTitleMap = new HashMap<>();
        if (!movieIds.isEmpty()) {
            List<Movie> movies = movieMapper.selectBatchIds(movieIds);
            movies.forEach(m -> movieTitleMap.put(m.getId(), m.getTitle()));
        }

        // 组装返回数据
        List<Map<String, Object>> result = orders.stream().map(o -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("orderNo", o.getOrderNo());
            map.put("movieId", o.getMovieId());
            map.put("movieTitle", movieTitleMap.getOrDefault(o.getMovieId(), "-"));
            map.put("amount", o.getAmount());
            double balancePaid = o.getBalancePaid() == null ? 0 : o.getBalancePaid();
            map.put("balancePaid", balancePaid);
            map.put("alipayPaid", o.getStatus() == 1 ? o.getAmount() - balancePaid : 0);
            map.put("orderType", o.getOrderType());
            map.put("status", o.getStatus());
            map.put("createTime", o.getCreateTime() != null ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(o.getCreateTime()) : "-");
            return map;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    /**
     * 单片购买下单
     *
     * @param movieId 电影ID
     * @return 订单号
     */
    @PostMapping("/create")
    public Result createOrder(@RequestParam Long movieId) {
        User user = UserContext.getUser();
        if (user == null) return Result.error("请先登录");

        Order order = orderService.createOrder(user.getId(), movieId);
        if (order == null) {
            return Result.error("下单失败");
        }
        return Result.success("订单已创建", order.getOrderNo());
    }

    /**
     * 取消订单（退回已扣余额）
     */
    @PostMapping("/cancel")
    @org.springframework.transaction.annotation.Transactional
    public Result cancelOrder(@RequestParam String orderNo) {
        User user = UserContext.getUser();
        if (user == null) return Result.error("未登录");

        Order order = orderService.getByOrderNo(orderNo);
        if (order == null || !order.getUserId().equals(user.getId())) {
            return Result.error("订单不存在");
        }
        if (order.getStatus() != 0) {
            return Result.error("只能取消待支付订单");
        }

        // 退回已扣余额
        if (order.getBalancePaid() != null && order.getBalancePaid() > 0) {
            userService.refundBalance(order.getUserId(), order.getBalancePaid());
        }

        order.setStatus(2); // 已取消
        order.setBalancePaid(0.0);
        orderMapper.updateById(order);
        return Result.success("订单已取消");
    }

    /**
     * 余额支付（支持混合支付：余额不足时扣光余额，剩余部分走支付宝）
     */
    @PostMapping("/payWithBalance")
    @org.springframework.transaction.annotation.Transactional
    public Result payWithBalance(@RequestParam String orderNo) {
        User user = UserContext.getUser();
        if (user == null) return Result.error("未登录");

        Order order = orderService.getByOrderNo(orderNo);
        if (order == null || !order.getUserId().equals(user.getId())) {
            return Result.error("订单不存在");
        }
        if (order.getStatus() != 0) {
            return Result.error("订单状态异常");
        }

        // 查余额和已付金额
        User freshUser = userService.getById(user.getId());
        double balance = freshUser.getBalance() == null ? 0 : freshUser.getBalance();
        double totalAmount = order.getAmount();
        double alreadyPaid = order.getBalancePaid() == null ? 0 : order.getBalancePaid();
        double remaining = totalAmount - alreadyPaid;  // 还需支付的金额

        if (balance <= 0) {
            return Result.error("余额为0，请选择其他支付方式");
        }

        if (balance >= remaining) {
            // 余额充足 → 扣除剩余金额 → 支付成功
            userService.deductBalance(user.getId(), remaining);
            order.setBalancePaid(totalAmount);
            orderService.payOrder(orderNo, 0, "BALANCE");
            return Result.success("余额支付成功");
        } else {
            // 余额不足 → 扣光余额 → 累加已扣金额 → 返回剩余让前端跳支付宝
            userService.deductBalance(user.getId(), balance);
            order.setBalancePaid(alreadyPaid + balance);
            orderMapper.updateById(order);
            double stillRemaining = remaining - balance;
            return Result.success("余额已扣¥" + String.format("%.2f", balance) + "，剩余¥" + String.format("%.2f", stillRemaining) + "需支付宝支付", stillRemaining);
        }
    }

    /**
     * 支付宝支付（扣除余额后的剩余部分）
     */
    @PostMapping("/payWithAlipay")
    public String payWithAlipay(@RequestParam String orderNo) {
        User user = UserContext.getUser();
        if (user == null) return "<h1>请先登录</h1>";

        Order order = orderService.getByOrderNo(orderNo);
        if (order == null || !order.getUserId().equals(user.getId())) {
            return "<h1>订单不存在</h1>";
        }
        if (order.getStatus() != 0) {
            return "<h1>订单状态异常</h1>";
        }

        // 计算需支付宝支付的金额 = 订单总额 - 已用余额支付的部分
        double balancePaid = order.getBalancePaid() == null ? 0 : order.getBalancePaid();
        double alipayAmount = order.getAmount() - balancePaid;

        if (alipayAmount <= 0) {
            return "<h1>无需支付宝支付</h1>";
        }

        try {
            com.software.movie.entity.dto.PaymentRequest request = new com.software.movie.entity.dto.PaymentRequest();
            request.setOrderNumber(orderNo);
            request.setAmount(new java.math.BigDecimal(String.valueOf(alipayAmount)));
            request.setProductName("电影购买");
            request.setDescription("订单号：" + orderNo);
            return alipayService.createPayment(request);
        } catch (Exception e) {
            return "<h1>支付创建失败: " + e.getMessage() + "</h1>";
        }
    }

    /**
     * 手动同步支付宝订单状态（回调丢失时使用）
     */
    @PostMapping("/syncStatus")
    public Result syncStatus(@RequestParam String orderNo) {
        User user = UserContext.getUser();
        if (user == null) return Result.error("未登录");

        Order order = orderService.getByOrderNo(orderNo);
        if (order == null || !order.getUserId().equals(user.getId())) {
            return Result.error("订单不存在");
        }
        if (order.getStatus() != 0) {
            return Result.error("订单已处理");
        }

        String tradeStatus = alipayService.queryTradeStatus(orderNo);
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            orderService.payOrder(orderNo, 1, "SYNC_" + orderNo);
            return Result.success("同步成功，订单已更新为已支付");
        } else if (tradeStatus == null) {
            return Result.error("查询失败，请稍后重试");
        } else {
            return Result.error("支付宝交易状态：" + tradeStatus);
        }
    }
}
