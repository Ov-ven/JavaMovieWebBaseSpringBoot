package com.software.movie.service.impl;

import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.Message;

import java.util.Collections;

/**
 * 秒杀事务监听器。
 * RocketMQ 发送 Half Message 后回调此监听器执行本地事务（Redis 库存扣减）。
 * 仅在配置了 rocketmq.name-server 时生效。
 */
@RocketMQTransactionListener
@ConditionalOnProperty(name = "rocketmq.name-server")
public class SecKillTransactionListener implements RocketMQLocalTransactionListener {

    private static final Logger log = LoggerFactory.getLogger(SecKillTransactionListener.class);

    private static final String STOCK_KEY_PREFIX = "seckill:stock:movie:";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private DefaultRedisScript<Long> seckillScript;

    /**
     * Half Message 发送成功后，MQ Broker 回调此方法执行本地事务。
     * 通过 Redis Lua 脚本原子扣减库存，成功则 COMMIT，失败则 ROLLBACK。
     *
     * @param msg RocketMQ 消息
     * @param arg 附加参数（未使用）
     * @return 事务状态：COMMIT（扣减成功）、ROLLBACK（扣减失败或异常）
     */
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        SecKillServiceImpl.SecKillMessage payload = JSON.parseObject(
                new String((byte[]) msg.getPayload()),
                SecKillServiceImpl.SecKillMessage.class
        );
        Long movieId = payload.getMovieId();
        Long userId = payload.getUserId();

        String stockKey = STOCK_KEY_PREFIX + movieId;

        try {
            Long result = stringRedisTemplate.execute(
                    seckillScript,
                    Collections.singletonList(stockKey)
            );

            if (result != null && result == 1) {
                log.info("本地事务执行成功：用户 {} 抢购电影 {}，库存已扣减，COMMIT 消息", userId, movieId);
                return RocketMQLocalTransactionState.COMMIT;
            }

            log.info("本地事务执行失败：用户 {} 抢购电影 {}，库存不足，ROLLBACK 消息", userId, movieId);
            return RocketMQLocalTransactionState.ROLLBACK;
        } catch (Exception e) {
            log.error("本地事务执行异常：用户 {} 抢购电影 {}，ROLLBACK 消息", userId, movieId, e);
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }

    /**
     * 兜底回查：当 Broker 未收到 COMMIT/ROLLBACK 时，定时回调此方法。
     * 当前实现无法精确判断（Redis 无扣减记录的用户维度标记），返回 UNKNOWN 等待重试。
     *
     * @param msg RocketMQ 消息
     * @return 始终返回 UNKNOWN，等待 Broker 自动重试或人工介入
     */
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        log.warn("事务回查触发，默认返回 UNKNOWN，消息: {}", new String((byte[]) msg.getPayload()));
        return RocketMQLocalTransactionState.UNKNOWN;
    }
}
