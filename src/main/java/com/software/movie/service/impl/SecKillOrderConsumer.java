package com.software.movie.service.impl;

import com.alibaba.fastjson.JSON;
import com.software.movie.entity.Movie;
import com.software.movie.entity.Order;
import com.software.movie.mapper.MovieMapper;
import com.software.movie.mapper.OrderMapper;
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

        // 构建秒杀订单并落库
        Order order = new Order();
        order.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
        order.setUserId(userId);
        order.setMovieId(movieId);
        order.setAmount(movie.getPrice());
        order.setOrderType(3);   // 秒杀订单
        order.setStatus(0);      // 待支付
        orderMapper.insert(order);

        log.info("秒杀订单落库成功：用户 {}，电影 {}，订单号 {}", userId, movieId, order.getOrderNo());
    }
}
