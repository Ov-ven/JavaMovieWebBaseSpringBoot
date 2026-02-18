package com.software.movie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.software.movie.entity.Order;
import com.software.movie.mapper.OrderMapper;
import com.software.movie.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;

    @Override
    public Order createOrder(Long userId, Double amount) {
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setAmount(amount);
        order.setStatus(0); // 未支付
        orderMapper.insert(order);
        return order;
    }

    @Override
    public boolean payOrder(String orderNo, Integer payType, String payNo) {
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.eq("order_no", orderNo);
        Order order = orderMapper.selectOne(wrapper);

        if (order != null && order.getStatus() == 0) {
            order.setStatus(1); // 已支付
            order.setPayType(payType);
            order.setPayNo(payNo);
            order.setPayTime(new Date());
            return orderMapper.updateById(order) > 0;
        }
        return false;
    }

    @Override
    public Order getByOrderNo(String orderNo) {
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.eq("order_no", orderNo);
        return orderMapper.selectOne(wrapper);
    }

    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }
}