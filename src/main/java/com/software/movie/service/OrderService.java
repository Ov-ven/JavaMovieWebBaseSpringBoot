package com.software.movie.service;

import com.software.movie.entity.Order;

public interface OrderService {
    Order createOrder(Long userId, Double amount);

    boolean payOrder(String orderNo, Integer payType, String payNo);

    Order getByOrderNo(String orderNo);
}