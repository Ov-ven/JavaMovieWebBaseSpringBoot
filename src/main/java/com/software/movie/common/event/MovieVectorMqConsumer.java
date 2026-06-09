package com.software.movie.common.event;

import com.software.movie.service.MovieVectorIngestionService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 电影向量同步 MQ 消费者。
 * <p>监听 RocketMQ 消息，执行大模型 API 向量同步（RAG 洗刷）。
 * 耗时操作在 MQ 消费线程中执行，不阻塞主业务线程。</p>
 * <p>RocketMQ 内置阶梯级延迟重试机制：消费失败抛出异常后，
 * 自动按 10s、30s、1m、2m、3m、... 间隔重试，默认最多 16 次。
 * 超过重试次数的消息进入死信队列（DLQ）供人工排查。</p>
 * <p>仅在配置了 rocketmq.name-server 时生效。</p>
 */
@Component
@ConditionalOnProperty(name = "rocketmq.name-server")
@RocketMQMessageListener(
        topic = "movie-vector-sync-topic",
        consumerGroup = "movie-vector-consumer-group"
)
public class MovieVectorMqConsumer implements RocketMQListener<MovieDataChangeEvent> {

    private static final Logger log = LoggerFactory.getLogger(MovieVectorMqConsumer.class);

    @Autowired
    private MovieVectorIngestionService ingestionService;

    @Override
    public void onMessage(MovieDataChangeEvent event) {
        log.info("MQ 消费者收到消息，开始执行大模型 API 向量同步: movieId={}, op={}",
                event.getMovieId(), event.getOperation());

        try {
            if (MovieDataChangeEvent.UPSERT.equals(event.getOperation())) {
                ingestionService.ingestSingleMovie(event.getMovieId());
            } else if (MovieDataChangeEvent.DELETE.equals(event.getOperation())) {
                ingestionService.removeSingleMovie(event.getMovieId());
            } else {
                log.warn("未知的操作类型，跳过: {}", event.getOperation());
            }
            log.info("向量同步执行完成: movieId={}", event.getMovieId());
        } catch (Exception e) {
            // 抛出运行时异常，RocketMQ 捕获后自动触发阶梯级延迟重试
            log.error("调用大模型向量 API 异常，触发 MQ 重试: movieId={}, error={}",
                    event.getMovieId(), e.getMessage());
            throw new RuntimeException("向量洗刷消费失败，要求 RocketMQ 重试", e);
        }
    }
}
