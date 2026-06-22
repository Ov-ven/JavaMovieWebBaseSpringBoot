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

import java.util.Arrays;

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
    private static final String STOCK_PREFIX = "seckill:";

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
        String userSetKey = STOCK_PREFIX + "users:movie:" + movieId;

        try {
            Long result = stringRedisTemplate.execute(
                    seckillScript,
                    Arrays.asList(stockKey, userSetKey),
                    userId.toString()
            );

            if (result != null && result == 1) {
                log.info("本地事务执行成功：用户 {} 抢购电影 {}，库存已扣减，COMMIT 消息", userId, movieId);
                return RocketMQLocalTransactionState.COMMIT;
            }

            if (result != null && result == 2) {
                log.info("本地事务执行：用户 {} 抢购电影 {}，重复请求，ROLLBACK 消息", userId, movieId);
                return RocketMQLocalTransactionState.ROLLBACK;
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
     * <p>通过检查 Redis 中的用户集合判断库存是否已扣减：
     * - 用户在集合中 → 已扣减 → COMMIT
     * - 用户不在集合中 → 未扣减 → ROLLBACK</p>
     *
     * @param msg RocketMQ 消息
     * @return COMMIT（已扣减）或 ROLLBACK（未扣减）
     */
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        SecKillServiceImpl.SecKillMessage payload = JSON.parseObject(
                new String((byte[]) msg.getPayload()),
                SecKillServiceImpl.SecKillMessage.class
        );
        Long movieId = payload.getMovieId();
        Long userId = payload.getUserId();

        String userSetKey = STOCK_PREFIX + "users:movie:" + movieId;

        try {
            Boolean isMember = stringRedisTemplate.opsForSet().isMember(userSetKey, userId.toString());
            if (Boolean.TRUE.equals(isMember)) {
                log.info("事务回查：用户 {} 电影 {} 已扣减库存，返回 COMMIT", userId, movieId);
                return RocketMQLocalTransactionState.COMMIT;
            }
            log.info("事务回查：用户 {} 电影 {} 未扣减库存，返回 ROLLBACK", userId, movieId);
            return RocketMQLocalTransactionState.ROLLBACK;
        } catch (Exception e) {
            log.error("事务回查异常：用户 {} 电影 {}，返回 UNKNOWN", userId, movieId, e);
            return RocketMQLocalTransactionState.UNKNOWN;
        }
    }
}
