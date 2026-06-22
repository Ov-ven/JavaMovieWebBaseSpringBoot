package com.software.movie.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.software.movie.common.Result;
import com.software.movie.entity.Order;
import com.software.movie.entity.User;
import com.software.movie.entity.UserMovieEntitlement;
import com.software.movie.mapper.OrderMapper;
import com.software.movie.mapper.UserMapper;
import com.software.movie.mapper.UserMovieEntitlementMapper;
import com.software.movie.service.OrderService;
import com.software.movie.service.SecKillService;
import com.software.movie.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 秒杀压测专用控制器（无鉴权）。
 * <p>仅用于 Apifox / JMeter 等工具模拟并发秒杀测试，
 * 生产环境应禁用或删除此类。</p>
 */
@RestController
@RequestMapping("/test/seckill")
public class SecKillTestController {

    private static final Logger log = LoggerFactory.getLogger(SecKillTestController.class);

    private static final String STOCK_KEY_PREFIX = "seckill:stock:movie:";

    @Autowired(required = false)
    private SecKillService secKillService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired(required = false)
    @Qualifier("seckillScript")
    private DefaultRedisScript<Long> seckillScript;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserMovieEntitlementMapper entitlementMapper;

    /**
     * 初始化秒杀库存（压测前调用）。
     *
     * @param movieId 电影ID
     * @param stock   库存数量（如 100）
     * @return 操作结果
     */
    @PostMapping("/init")
    public Result initStock(@RequestParam Long movieId, @RequestParam Integer stock) {
        String key = STOCK_KEY_PREFIX + movieId;
        stringRedisTemplate.opsForValue().set(key, String.valueOf(stock));
        log.info("秒杀库存初始化: movieId={}, stock={}", movieId, stock);
        return Result.success("库存初始化成功", Map.of("movieId", movieId, "stock", stock));
    }

    /**
     * 查询秒杀库存。
     */
    @GetMapping("/stock")
    public Result getStock(@RequestParam Long movieId) {
        String key = STOCK_KEY_PREFIX + movieId;
        String val = stringRedisTemplate.opsForValue().get(key);
        int stock = val != null ? Integer.parseInt(val) : 0;
        return Result.success(Map.of("movieId", movieId, "stock", stock));
    }

    /**
     * 执行秒杀（压测专用，免鉴权，直接传 userId）。
     *
     * @param userId  用户ID
     * @param movieId 电影ID
     * @return 秒杀结果
     */
    @PostMapping("/do")
    public Result doSeckill(@RequestParam Long userId, @RequestParam Long movieId) {
        // 方案一：走完整 RocketMQ 事务消息链路
        if (secKillService != null) {
            try {
                boolean submitted = secKillService.executeSeckill(movieId, userId);
                if (submitted) {
                    return Result.success("秒杀请求已提交（MQ 异步处理）", Map.of(
                            "userId", userId, "movieId", movieId, "mode", "rocketmq"
                    ));
                } else {
                    return Result.error("秒杀请求提交失败（库存不足或重复请求）");
                }
            } catch (Exception e) {
                return Result.error("秒杀请求失败: " + e.getMessage());
            }
        }

        // 方案二：无 RocketMQ 时，直接用 Lua 脚本扣减（压测降级）
        if (seckillScript == null) {
            return Result.error("秒杀脚本未配置");
        }

        String stockKey = STOCK_KEY_PREFIX + movieId;
        String userSetKey = "seckill:users:movie:" + movieId;
        try {
            Long result = stringRedisTemplate.execute(seckillScript,
                    Arrays.asList(stockKey, userSetKey), userId.toString());
            if (result != null && result == 1) {
                log.info("秒杀成功: userId={}, movieId={}", userId, movieId);
                return Result.success("秒杀成功", Map.of(
                        "userId", userId, "movieId", movieId, "mode", "lua-direct"
                ));
            } else if (result != null && result == 2) {
                return Result.error("重复请求，您已抢过该电影");
            } else {
                return Result.error("库存不足，秒杀失败");
            }
        } catch (Exception e) {
            log.error("秒杀执行异常: userId={}, movieId={}, error={}", userId, movieId, e.getMessage());
            return Result.error("秒杀执行异常: " + e.getMessage());
        }
    }

    /**
     * 批量查询秒杀结果（压测后验证）。
     */
    @GetMapping("/result")
    public Result getResult(@RequestParam Long movieId) {
        QueryWrapper<Order> paidWrapper = new QueryWrapper<>();
        paidWrapper.eq("movie_id", movieId).eq("order_type", 3).eq("status", 1);
        Long paidCount = orderMapper.selectCount(paidWrapper);

        QueryWrapper<Order> totalWrapper = new QueryWrapper<>();
        totalWrapper.eq("movie_id", movieId).eq("order_type", 3);
        Long totalCount = orderMapper.selectCount(totalWrapper);

        String key = STOCK_KEY_PREFIX + movieId;
        String val = stringRedisTemplate.opsForValue().get(key);
        int remainingStock = val != null ? Integer.parseInt(val) : 0;

        Map<String, Object> data = new HashMap<>();
        data.put("movieId", movieId);
        data.put("remainingStock", remainingStock);
        data.put("paidOrders", paidCount);
        data.put("totalOrders", totalCount);

        return Result.success(data);
    }

    /**
     * 批量创建测试用户（压测前调用，解决外键约束问题）。
     * 用户 ID 从 900001 开始递增，密码统一为 123456。
     *
     * @param count 创建数量
     * @return 操作结果
     */
    @PostMapping("/createUsers")
    public Result createTestUsers(@RequestParam(defaultValue = "100") Integer count) {
        int created = 0;
        String lastError = null;
        for (int i = 1; i <= count; i++) {
            long uid = 900000L + i;
            User u = new User();
            u.setId(uid);
            u.setUsername("testuser" + uid);
            u.setPassword("e10adc3949ba59abbe56e057f20f883e"); // MD5(123456)
            u.setIsvip(0);
            u.setStatus(true);
            try {
                int rows = userMapper.insert(u);
                if (rows > 0) created++;
            } catch (Exception e) {
                lastError = e.getMessage();
                log.warn("创建测试用户失败: uid={}, error={}", uid, e.getMessage());
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("created", created);
        data.put("startId", 900001);
        data.put("endId", 900000 + count);
        if (lastError != null) data.put("lastError", lastError);
        return Result.success("创建完成", data);
    }

    /**
     * 清理测试数据（压测后调用）。
     */
    @PostMapping("/cleanup")
    public Result cleanup(@RequestParam Long movieId) {
        String key = STOCK_KEY_PREFIX + movieId;
        stringRedisTemplate.delete(key);

        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.eq("movie_id", movieId).eq("order_type", 3);
        orderMapper.delete(wrapper);

        log.info("测试数据清理完成: movieId={}", movieId);
        return Result.success("清理完成");
    }

    /**
     * 批量给测试用户充值（全链路测试用）。
     *
     * @param startId 起始用户 ID
     * @param count   用户数量
     * @param amount  每人充值金额
     * @return 操作结果
     */
    @PostMapping("/batchRecharge")
    public Result batchRecharge(@RequestParam(defaultValue = "900001") Long startId,
                                @RequestParam(defaultValue = "200") Integer count,
                                @RequestParam(defaultValue = "1000") Double amount) {
        int success = 0;
        int failed = 0;
        for (long uid = startId; uid < startId + count; uid++) {
            try {
                boolean ok = userService.recharge(uid, amount);
                if (ok) success++; else failed++;
            } catch (Exception e) {
                failed++;
            }
        }
        log.info("批量充值完成: startId={}, count={}, amount={}, success={}, failed={}", startId, count, amount, success, failed);
        return Result.success("充值完成", Map.of("success", success, "failed", failed, "amount", amount));
    }

    /**
     * 批量用余额支付秒杀订单（全链路测试用）。
     * <p>查询所有 orderType=3 且 status=0 的待支付订单，逐个用余额支付。</p>
     *
     * @param movieId 电影 ID
     * @return 操作结果
     */
    @PostMapping("/batchPay")
    public Result batchPay(@RequestParam Long movieId) {
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.eq("movie_id", movieId).eq("order_type", 3).eq("status", 0);
        List<Order> pendingOrders = orderMapper.selectList(wrapper);

        int paid = 0;
        int failed = 0;
        for (Order order : pendingOrders) {
            try {
                // 用余额支付
                boolean deducted = userService.deductBalance(order.getUserId(), order.getAmount());
                if (deducted) {
                    // 更新订单状态 + 写入资产表
                    orderService.payOrder(order.getOrderNo(), 0, "BALANCE_TEST");
                    paid++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                failed++;
                log.warn("批量支付失败: orderNo={}, error={}", order.getOrderNo(), e.getMessage());
            }
        }
        log.info("批量支付完成: movieId={}, total={}, paid={}, failed={}", movieId, pendingOrders.size(), paid, failed);
        return Result.success("支付完成", Map.of("total", pendingOrders.size(), "paid", paid, "failed", failed));
    }
}
