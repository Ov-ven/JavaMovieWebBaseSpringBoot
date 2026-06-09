package com.software.movie.common.event;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 电影向量同步监听器（MQ 生产者）。
 * <p>监听电影数据变更事件，在 MySQL 事务提交后将事件发送到 RocketMQ，
 * 由下游消费者异步执行大模型 API 向量同步，避免阻塞主线程。</p>
 * <p>仅在配置了 rocketmq.name-server 时生效。</p>
 */
@Component
@ConditionalOnProperty(name = "rocketmq.name-server")
public class MovieVectorSyncListener {

    private static final Logger log = LoggerFactory.getLogger(MovieVectorSyncListener.class);

    private static final String TOPIC = "movie-vector-sync-topic";

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 处理电影数据变更事件。
     * <p>使用 @TransactionalEventListener 确保 MySQL 事务提交后才触发，
     * 将事件序列化发送到 RocketMQ，由消费者异步执行向量同步。</p>
     *
     * @param event 电影数据变更事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMovieDataChange(MovieDataChangeEvent event) {
        log.info("监听到 MySQL 事务提交，准备发送向量同步 MQ 消息: movieId={}, op={}",
                event.getMovieId(), event.getOperation());

        try {
            rocketMQTemplate.convertAndSend(TOPIC, event);
            log.info("MQ 消息发送成功: movieId={}", event.getMovieId());
        } catch (Exception e) {
            // 发送失败仅打日志，不影响主业务流程
            // 生产环境建议接入本地消息表或告警通知
            log.error("MQ 消息发送失败: movieId={}, error={}",
                    event.getMovieId(), e.getMessage(), e);
        }
    }
}
