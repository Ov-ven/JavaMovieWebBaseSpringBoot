package com.software.movie.common.event;

import com.software.movie.service.MovieVectorIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 电影向量同步监听器。
 * <p>监听电影数据变更事件，在 MySQL 事务提交后异步更新 Redis 向量库。</p>
 */
@Component
public class MovieVectorSyncListener {

    private static final Logger log = LoggerFactory.getLogger(MovieVectorSyncListener.class);

    @Autowired
    private MovieVectorIngestionService ingestionService;

    /**
     * 处理电影数据变更事件。
     * <p>使用 @TransactionalEventListener 确保 MySQL 事务提交后才触发，
     * 使用 @Async 在异步线程池中执行，不阻塞主线程。</p>
     *
     * @param event 电影数据变更事件
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMovieDataChange(MovieDataChangeEvent event) {
        try {
            log.info("收到电影数据变更事件：movieId={}, operation={}", event.getMovieId(), event.getOperation());

            if (MovieDataChangeEvent.UPSERT.equals(event.getOperation())) {
                ingestionService.ingestSingleMovie(event.getMovieId());
            } else if (MovieDataChangeEvent.DELETE.equals(event.getOperation())) {
                ingestionService.removeSingleMovie(event.getMovieId());
            } else {
                log.warn("未知的操作类型：{}", event.getOperation());
            }
        } catch (Exception e) {
            // 异步线程中的异常不能抛出，否则会影响主线程
            log.error("电影向量同步失败：movieId={}, operation={}, error={}",
                    event.getMovieId(), event.getOperation(), e.getMessage(), e);
        }
    }
}
