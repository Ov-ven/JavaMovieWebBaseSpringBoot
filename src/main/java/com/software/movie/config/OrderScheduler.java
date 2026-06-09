package com.software.movie.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.software.movie.entity.Order;
import com.software.movie.mapper.OrderMapper;
import com.software.movie.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 订单定时任务。
 * 定期扫描并取消超过30分钟未支付的待支付订单，同时退回已扣余额。
 */
@Component
public class OrderScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderScheduler.class);

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserService userService;

    /**
     * 每5分钟执行一次，取消超过30分钟的待支付订单，并退回已扣余额
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void cancelExpiredOrders() {
        Date expireTime = new Date(System.currentTimeMillis() - 30 * 60 * 1000);
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 0)
               .lt("create_time", expireTime);

        List<Order> expiredOrders = orderMapper.selectList(wrapper);
        if (expiredOrders.isEmpty()) return;

        int refundCount = 0;
        for (Order order : expiredOrders) {
            // 如果有余额扣减，先退回
            if (order.getBalancePaid() != null && order.getBalancePaid() > 0) {
                userService.refundBalance(order.getUserId(), order.getBalancePaid());
                log.info("过期订单 {} 退回余额 ¥{} 给用户 {}", order.getOrderNo(), order.getBalancePaid(), order.getUserId());
                order.setBalancePaid(0.0);
                refundCount++;
            }
            order.setStatus(2); // 已取消
            orderMapper.updateById(order);
        }
        log.info("自动取消过期待支付订单 {} 笔，其中退回余额 {} 笔", expiredOrders.size(), refundCount);
    }
}
