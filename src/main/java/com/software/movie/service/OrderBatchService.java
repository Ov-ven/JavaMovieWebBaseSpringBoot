package com.software.movie.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.software.movie.entity.Order;
import com.software.movie.mapper.OrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;

/**
 * 订单批处理服务。
 * <p>从 OrderScheduler 中抽取出来，保证 @Transactional 通过 Spring AOP 代理生效。</p>
 *
 * <h3>Redis 与 MySQL 一致性设计</h3>
 * <p>Redis 操作不在事务循环内执行，而是收集待退回的库存信息，
 * 利用 {@code TransactionSynchronization.afterCommit()} 钩子，
 * 在 MySQL 事务 100% 提交后才批量写回 Redis。</p>
 * <p>若 MySQL 事务回滚，afterCommit 不触发，Redis 不会被修改。</p>
 */
@Service
public class OrderBatchService {

    private static final Logger log = LoggerFactory.getLogger(OrderBatchService.class);

    private static final String STOCK_KEY_PREFIX = "seckill:stock:movie:";

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 方案 A：大事务 + 后置提交。
     * <p>整批订单在同一个 MySQL 事务中处理，Redis 库存在事务提交后才退回。</p>
     *
     * @param orders 本批次的过期订单
     * @return int[]{取消笔数, 退款笔数}
     */
    @Transactional
    public int[] processBatch(List<Order> orders) {
        int cancelled = 0;
        int refunded = 0;

        // 收集需要退回 Redis 库存的 movieId → 数量（不在事务内操作 Redis）
        Map<Long, Integer> stockReturnMap = new HashMap<>();

        for (Order order : orders) {
            // CAS 取消：只有待支付(0)的订单才能取消，避免和支付回调冲突
            QueryWrapper<Order> casWrapper = new QueryWrapper<>();
            casWrapper.eq("id", order.getId()).eq("status", 0);
            Order cancelUpdate = new Order();
            cancelUpdate.setStatus(2); // 已取消
            cancelUpdate.setBalancePaid(0.0);
            int rows = orderMapper.update(cancelUpdate, casWrapper);
            if (rows == 0) {
                log.info("订单 {} 已被支付或已取消，跳过", order.getOrderNo());
                continue; // 已被支付回调抢走，跳过
            }

            // 退回余额（CAS 成功才退）
            if (order.getBalancePaid() != null && order.getBalancePaid() > 0) {
                userService.refundBalance(order.getUserId(), order.getBalancePaid());
                log.info("过期订单 {} 退回余额 ¥{} 给用户 {}", order.getOrderNo(), order.getBalancePaid(), order.getUserId());
                refunded++;
            }
            // 收集秒杀库存退回信息（CAS 成功才收集）
            if (order.getOrderType() != null && order.getOrderType() == 3 && order.getMovieId() != null) {
                stockReturnMap.merge(order.getMovieId(), 1, Integer::sum);
            }
            cancelled++;
        }

        // 注册事务提交后钩子：MySQL 提交成功后才操作 Redis
        if (!stockReturnMap.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    stockReturnMap.forEach((movieId, count) -> {
                        String stockKey = STOCK_KEY_PREFIX + movieId;
                        stringRedisTemplate.opsForValue().increment(stockKey, count);
                        log.info("事务提交后退回 Redis 库存: movieId={}, +{}", movieId, count);
                    });
                }
            });
        }

        return new int[]{cancelled, refunded};
    }

    /**
     * 方案 B：拆解为独立单条小事务。
     * <p>每条订单独立事务，单条失败不影响其他订单。
     * MySQL 成功后即时操作 Redis，失败则跳过。</p>
     * <p>适用于允许部分成功、部分失败的场景。</p>
     *
     * @param orders 本批次的过期订单
     * @return int[]{取消笔数, 退款笔数}
     */
    public int[] processBatchIndividually(List<Order> orders) {
        int cancelled = 0;
        int refunded = 0;

        for (Order order : orders) {
            try {
                int[] result = processSingleOrder(order);
                cancelled += result[0];
                refunded += result[1];
            } catch (Exception e) {
                log.error("单条订单处理失败，跳过: orderNo={}, error={}", order.getOrderNo(), e.getMessage());
            }
        }

        return new int[]{cancelled, refunded};
    }

    /**
     * 处理单条过期订单（独立事务）。
     * <p>MySQL 操作和 Redis 操作在同一个方法内，事务提交后 Redis 才生效。
     * 注意：此处 Redis 操作仍在事务内，但因为是单条操作，影响范围极小。</p>
     */
    @Transactional
    public int[] processSingleOrder(Order order) {
        int cancelled = 0;
        int refunded = 0;

        // 退回余额
        if (order.getBalancePaid() != null && order.getBalancePaid() > 0) {
            userService.refundBalance(order.getUserId(), order.getBalancePaid());
            log.info("过期订单 {} 退回余额 ¥{} 给用户 {}", order.getOrderNo(), order.getBalancePaid(), order.getUserId());
            order.setBalancePaid(0.0);
            refunded++;
        }

        order.setStatus(2); // 已取消
        orderMapper.updateById(order);
        cancelled++;

        // 秒杀订单退回 Redis 库存（单条事务，即使回滚影响也极小）
        if (order.getOrderType() != null && order.getOrderType() == 3 && order.getMovieId() != null) {
            String stockKey = STOCK_KEY_PREFIX + order.getMovieId();
            stringRedisTemplate.opsForValue().increment(stockKey);
            log.info("秒杀订单 {} 超时取消，退回库存 movieId={}", order.getOrderNo(), order.getMovieId());
        }

        return new int[]{cancelled, refunded};
    }
}
