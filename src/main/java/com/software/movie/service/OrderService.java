package com.software.movie.service;

import com.software.movie.entity.Order;

import java.util.List;

/**
 * 订单服务接口。
 * 提供单片购买、VIP购买的订单创建，订单支付、查询及取消功能。
 */
public interface OrderService {

    /**
     * 单片购买下单
     *
     * @param userId  用户ID（显式传入，不依赖隐式上下文）
     * @param movieId 电影ID
     * @return 订单实体（含 orderNo）
     */
    Order createOrder(Long userId, Long movieId);

    /**
     * 支付订单，更新订单状态为已支付
     *
     * @param orderNo 我方订单号
     * @param payType 支付方式（1-支付宝 2-微信）
     * @param payNo   第三方支付交易号
     * @return 操作是否成功
     */
    boolean payOrder(String orderNo, Integer payType, String payNo);

    /**
     * 根据订单号查询订单
     *
     * @param orderNo 我方订单号
     * @return 订单实体，不存在时返回 null
     */
    Order getByOrderNo(String orderNo);

    /**
     * 根据用户ID和电影ID查询订单（秒杀结果轮询）
     *
     * @param userId  用户ID
     * @param movieId 电影ID
     * @return 最近一笔待支付或已支付的订单，不存在时返回 null
     */
    Order getByUserIdAndMovieId(Long userId, Long movieId);

    /**
     * 根据用户ID和电影ID查询未过期的待支付订单
     */
    Order getPendingOrder(Long userId, Long movieId);

    /**
     * 根据用户ID和电影ID查询已支付的订单（用于观看权限判断）
     *
     * @param userId  用户ID
     * @param movieId 电影ID
     * @return 已支付的订单，不存在时返回 null
     */
    Order getPaidOrder(Long userId, Long movieId);

    /**
     * 创建VIP购买订单
     *
     * @param userId 用户ID
     * @param amount 订单金额
     * @return 订单实体（含 orderNo）
     */
    Order createVipOrder(Long userId, Double amount);

    /**
     * 查询用户订单列表（可按状态筛选）
     *
     * @param userId 用户ID
     * @param status 订单状态（null=查全部，0=待支付，1=已支付，2=已取消）
     * @return 订单列表
     */
    List<Order> getOrdersByUserId(Long userId, Integer status);

    /**
     * 取消订单（仅待支付状态可取消，退回已扣余额）
     *
     * @param userId  用户ID
     * @param orderNo 订单号
     * @return 取消结果描述
     */
    String cancelOrder(Long userId, String orderNo);

    /**
     * 使用余额支付待支付订单（余额不足时返回剩余金额信息）
     *
     * @param userId  用户ID
     * @param orderNo 订单号
     * @return 支付结果描述（成功/余额不足需补差/失败原因）
     */
    String payOrderWithBalance(Long userId, String orderNo);
}
