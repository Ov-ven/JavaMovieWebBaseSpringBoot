package com.software.movie.service.impl;

import com.alibaba.fastjson.JSON;
import com.software.movie.service.SecKillService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

/**
 * 秒杀服务实现类。
 * 基于 RocketMQ 事务消息实现秒杀流程：发送 Half Message 后由
 * {@link SecKillTransactionListener} 执行本地 Redis 库存扣减，
 * 消费端 {@link SecKillOrderConsumer} 异步完成 MySQL 订单落库。
 * 仅在配置了 rocketmq.name-server 时生效。
 */
@Service
@ConditionalOnProperty(name = "rocketmq.name-server")
public class SecKillServiceImpl implements SecKillService {

    private static final Logger log = LoggerFactory.getLogger(SecKillServiceImpl.class);

    private static final String TOPIC = "seckill-order-topic";

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 执行秒杀请求，发送 RocketMQ 事务消息。
     * 注意：此方法始终返回 true，实际扣减结果由事务监听器决定。
     *
     * @param movieId 电影ID
     * @param userId  用户ID
     * @return 始终返回 true（消息发送成功）
     */
    @Override
    public boolean executeSeckill(Long movieId, Long userId) {
        log.info("用户 {} 发起秒杀请求，电影 {}，发送事务消息", userId, movieId);

        // 构建消息体
        String payload = JSON.toJSONString(new SecKillMessage(movieId, userId));
        Message<String> message = MessageBuilder.withPayload(payload).build();

        // 发送事务消息（Half Message）
        // 本地事务逻辑在 SecKillTransactionListener.executeLocalTransaction 中执行
        rocketMQTemplate.sendMessageInTransaction(TOPIC, message, null);

        return true;
    }

    /**
     * 秒杀消息体，用于在 RocketMQ 消息中传递秒杀请求参数。
     */
    public static class SecKillMessage {
        private Long movieId;
        private Long userId;

        public SecKillMessage() {}

        public SecKillMessage(Long movieId, Long userId) {
            this.movieId = movieId;
            this.userId = userId;
        }

        public Long getMovieId() { return movieId; }
        public void setMovieId(Long movieId) { this.movieId = movieId; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
    }
}
