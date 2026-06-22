package com.software.movie.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.software.movie.entity.Order;
import com.software.movie.entity.User;
import com.software.movie.mapper.OrderMapper;
import com.software.movie.mapper.UserMapper;
import com.software.movie.service.OrderBatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 订单定时任务。
 * 定期扫描并取消超过30分钟未支付的待支付订单，同时退回已扣余额。
 * <p>使用 ReentrantLock 防止同 JVM 内并发执行，
 * 分批处理避免大批量订单同时到期时一次性加载过多数据。</p>
 */
@Component
public class OrderScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderScheduler.class);

    /** 每批处理的订单数量 */
    private static final int BATCH_SIZE = 100;

    /** 防止定时任务并发执行的锁 */
    private final ReentrantLock lock = new ReentrantLock();

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderBatchService orderBatchService;

    /**
     * 每5分钟执行一次，取消超过30分钟的待支付订单，并退回已扣余额。
     * <p>使用 tryLock 非阻塞获取锁，获取失败说明上一轮尚未执行完，直接跳过。</p>
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void cancelExpiredOrders() {
        if (!lock.tryLock()) {
            log.warn("上一轮过期订单处理尚未完成，本次跳过");
            return;
        }
        try {
            doCancelExpiredOrders();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 分批查询并取消过期订单。
     * <p>每次查询 {@link #BATCH_SIZE} 条，通过 OrderBatchService 处理（独立事务），
     * 循环直到没有剩余过期订单。</p>
     */
    private void doCancelExpiredOrders() {
        Date expireTime = new Date(System.currentTimeMillis() - 30 * 60 * 1000);
        int totalCancelled = 0;
        int totalRefunded = 0;

        while (true) {
            List<Order> batch = fetchExpiredBatch(expireTime);
            if (batch.isEmpty()) break;

            // 通过代理调用，保证 @Transactional 生效
            int[] result = orderBatchService.processBatch(batch);
            totalCancelled += result[0];
            totalRefunded += result[1];

            // 不足一批说明已经处理完所有过期订单
            if (batch.size() < BATCH_SIZE) break;
        }

        if (totalCancelled > 0) {
            log.info("自动取消过期待支付订单 {} 笔，其中退回余额 {} 笔", totalCancelled, totalRefunded);
        }
    }

    /**
     * 查询一批过期的待支付订单。
     */
    private List<Order> fetchExpiredBatch(Date expireTime) {
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 0)
               .lt("create_time", expireTime)
               .last("LIMIT " + BATCH_SIZE);
        return orderMapper.selectList(wrapper);
    }

    // ========== VIP 过期清理 ==========

    /**
     * 每天凌晨 2 点执行，批量将已过期的 VIP 用户降级。
     * <p>与 UserService.isVip() 的读时降级互补：定时任务保证数据库干净，
     * 读时判断保证业务逻辑实时正确。</p>
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void downgradeExpiredVip() {
        UpdateWrapper<User> wrapper = new UpdateWrapper<>();
        wrapper.eq("isvip", 1)
               .lt("vip_expire_time", new Date())
               .set("isvip", 0);
        int rows = userMapper.update(null, wrapper);
        if (rows > 0) {
            log.info("VIP 过期清理：批量降级 {} 名用户", rows);
        }
    }
}
