package com.software.movie.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.software.movie.entity.Movie;
import com.software.movie.entity.Order;
import com.software.movie.entity.UserMovieEntitlement;
import com.software.movie.mapper.MovieMapper;
import com.software.movie.mapper.OrderMapper;
import com.software.movie.mapper.UserMovieEntitlementMapper;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 秒杀订单消费者。
 * 监听 RocketMQ 秒杀订单队列，异步完成 MySQL 订单落库（削峰填谷）。
 * 仅在配置了 rocketmq.name-server 时生效。
 * <p>配置最大重试次数为 3 次，超过后消息进入死信队列（DLQ）供人工排查。</p>
 */
@Component
@ConditionalOnProperty(name = "rocketmq.name-server")
@RocketMQMessageListener(
        topic = "seckill-order-topic",
        consumerGroup = "seckill-consumer-group",
        maxReconsumeTimes = 3
)
public class SecKillOrderConsumer implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(SecKillOrderConsumer.class);

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private MovieMapper movieMapper;

    @Autowired
    private UserMovieEntitlementMapper entitlementMapper;

    /**
     * 消费秒杀订单消息，创建待支付订单并落库。
     *
     * @param body 消息体（JSON 格式的 SecKillMessage）
     */
    @Override
    public void onMessage(String body) {
        SecKillServiceImpl.SecKillMessage msg = JSON.parseObject(body, SecKillServiceImpl.SecKillMessage.class);
        Long movieId = msg.getMovieId();
        Long userId = msg.getUserId();

        log.info("消费者收到秒杀消息：用户 {}，电影 {}", userId, movieId);

        // 查询电影价格
        Movie movie = movieMapper.selectById(movieId);
        if (movie == null) {
            log.warn("电影 {} 不存在，丢弃消息", movieId);
            return;
        }

        // 幂等校验 1：用户是否已拥有该电影（普通购买/秒杀购买后已支付）
        QueryWrapper<UserMovieEntitlement> entWrapper = new QueryWrapper<>();
        entWrapper.eq("user_id", userId).eq("movie_id", movieId);
        if (entitlementMapper.selectCount(entWrapper) > 0) {
            log.info("用户已拥有该电影，跳过秒杀: userId={}, movieId={}", userId, movieId);
            return;
        }

        // 幂等校验 2：同一用户同一电影的秒杀订单只允许一条
        QueryWrapper<Order> checkWrapper = new QueryWrapper<>();
        checkWrapper.eq("user_id", userId).eq("movie_id", movieId).eq("order_type", 3);
        if (orderMapper.selectCount(checkWrapper) > 0) {
            log.info("秒杀订单已存在，跳过重复消费: userId={}, movieId={}", userId, movieId);
            return;
        }

        // 构建秒杀订单并落库（使用秒杀价，没有秒杀价则用原价）
        double price = (movie.getSeckillPrice() != null && movie.getSeckillPrice() > 0)
                ? movie.getSeckillPrice() : movie.getPrice();
        Order order = new Order();
        order.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
        order.setUserId(userId);
        order.setMovieId(movieId);
        order.setAmount(price);
        order.setOrderType(3);   // 秒杀订单
        order.setStatus(0);      // 待支付
        try {
            orderMapper.insert(order);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            log.info("秒杀订单重复插入（唯一索引兜底），跳过: userId={}, movieId={}", userId, movieId);
            return;
        }

        log.info("秒杀订单落库成功：用户 {}，电影 {}，订单号 {}", userId, movieId, order.getOrderNo());
    }
}
