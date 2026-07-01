package com.software.movie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.software.movie.common.BusinessException;
import com.software.movie.entity.Movie;
import com.software.movie.entity.Order;
import com.software.movie.entity.User;
import com.software.movie.entity.UserMovieEntitlement;
import com.software.movie.mapper.MovieMapper;
import com.software.movie.mapper.OrderMapper;
import com.software.movie.mapper.UserMovieEntitlementMapper;
import com.software.movie.service.OrderService;
import com.software.movie.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 订单服务实现类。
 * <p>提供单片购买和VIP购买的订单创建、支付状态更新及订单查询功能。
 * 包含应用层分布式锁 + 数据层凭证表双重防重。</p>
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    /** 分布式锁 Key 前缀 */
    private static final String LOCK_PREFIX = "movie:order:lock:";

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private MovieMapper movieMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private UserMovieEntitlementMapper entitlementMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 单片购买下单（应用层分布式锁 + 数据层幂等校验）。
     *
     * @param userId  用户ID
     * @param movieId 电影ID
     * @return 订单实体（含 orderNo）
     * @throws BusinessException 电影不存在/下架、免费影片、VIP影片、重复下单或并发冲突时抛出
     */
    @Override
    @Transactional
    public Order createOrder(Long userId, Long movieId) {
        String lockKey = LOCK_PREFIX + userId + ":" + movieId;
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);

        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException("正在处理中，请勿频繁点击");
        }

        try {
            return doCreateOrder(userId, movieId);
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    /**
     * 下单核心逻辑（在分布式锁保护下执行）。
     */
    private Order doCreateOrder(Long userId, Long movieId) {
        // 1. 凭证校验：是否已拥有该电影
        QueryWrapper<UserMovieEntitlement> entWrapper = new QueryWrapper<>();
        entWrapper.eq("user_id", userId).eq("movie_id", movieId);
        if (entitlementMapper.selectCount(entWrapper) > 0) {
            throw new BusinessException("您已购买该影片，无需重复购买");
        }

        // 2. 校验电影
        Movie movie = movieMapper.selectById(movieId);
        if (movie == null || Boolean.FALSE.equals(movie.getStatus())) {
            throw new BusinessException("电影不存在或已下架");
        }

        // 3. 查询用户
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 4. 判断是否需要购买
        if (movie.getPrice() == null || movie.getPrice() == 0) {
            throw new BusinessException("该电影为免费影片，无需购买");
        }
        if (movie.getIsVip() == 1 && userService.isVip(userId)) {
            throw new BusinessException("您是VIP用户，可直接观看VIP影片，无需购买");
        }

        // 5. 订单层幂等：检查是否已有待支付订单
        QueryWrapper<Order> checkWrapper = new QueryWrapper<>();
        checkWrapper.eq("user_id", userId)
                    .eq("movie_id", movieId)
                    .eq("status", 0);
        if (orderMapper.selectCount(checkWrapper) > 0) {
            throw new BusinessException("您已有一笔待支付订单，请勿重复下单");
        }

        // 6. 生成订单号并落库
        String orderNo = UUID.randomUUID().toString().replace("-", "");
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setMovieId(movieId);
        order.setAmount(movie.getPrice());
        order.setOrderType(1);
        order.setStatus(0);

        try {
            orderMapper.insert(order);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new BusinessException("请勿重复提交订单");
        }

        log.info("订单创建成功：用户 {}，电影 {}，订单号 {}", userId, movieId, orderNo);
        return order;
    }

    /**
     * 支付订单：更新状态 + 插入已购凭证。
     *
     * @param orderNo 我方订单号
     * @param payType 支付方式（0-余额 1-支付宝 2-微信）
     * @param payNo   第三方支付交易号
     * @return 操作是否成功
     */
    @Override
    @Transactional
    public boolean payOrder(String orderNo, Integer payType, String payNo) {
        // CAS 更新：只有待支付(0)的订单才能改为已支付(1)
        // 如果定时任务已经改为已取消(2)，这里影响行数为 0，不会冲突
        QueryWrapper<Order> updateWrapper = new QueryWrapper<>();
        updateWrapper.eq("order_no", orderNo).eq("status", 0);
        Order update = new Order();
        update.setStatus(1);
        update.setPayType(payType);
        update.setPayNo(payNo);
        update.setPayTime(new Date());
        int rows = orderMapper.update(update, updateWrapper);
        if (rows == 0) {
            return false; // 订单已被取消或已支付
        }

        // 查询更新后的订单（用于后续逻辑）
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.eq("order_no", orderNo);
        Order order = orderMapper.selectOne(wrapper);

        // 2. 插入已购凭证（user_movie_entitlement）
        if (order.getMovieId() != null) {
            UserMovieEntitlement entitlement = new UserMovieEntitlement();
            entitlement.setUserId(order.getUserId());
            entitlement.setMovieId(order.getMovieId());
            try {
                entitlementMapper.insert(entitlement);
                log.info("已购凭证插入成功：用户 {}，电影 {}", order.getUserId(), order.getMovieId());
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // 凭证已存在（幂等），忽略
                log.info("已购凭证已存在，跳过：用户 {}，电影 {}", order.getUserId(), order.getMovieId());
            }
        }

        return true;
    }

    @Override
    public Order getByOrderNo(String orderNo) {
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.eq("order_no", orderNo);
        return orderMapper.selectOne(wrapper);
    }

    @Override
    public Order getByUserIdAndMovieId(Long userId, Long movieId) {
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .eq("movie_id", movieId)
               .in("status", 0, 1)
               .orderByDesc("create_time")
               .last("LIMIT 1");
        return orderMapper.selectOne(wrapper);
    }

    @Override
    public Order getPaidOrder(Long userId, Long movieId) {
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .eq("movie_id", movieId)
               .eq("status", 1)
               .last("LIMIT 1");
        return orderMapper.selectOne(wrapper);
    }

    @Override
    public Order getPendingOrder(Long userId, Long movieId) {
        Date expireTime = new Date(System.currentTimeMillis() - 30 * 60 * 1000);
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .eq("movie_id", movieId)
               .eq("status", 0)
               .ge("create_time", expireTime)
               .last("LIMIT 1");
        return orderMapper.selectOne(wrapper);
    }

    @Override
    @Transactional
    public Order createVipOrder(Long userId, Double amount) {
        Order order = new Order();
        order.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
        order.setUserId(userId);
        order.setAmount(amount);
        order.setOrderType(2);
        order.setStatus(0);
        orderMapper.insert(order);
        return order;
    }

    /**
     * 查询用户订单列表（可按状态筛选）
     */
    @Override
    public java.util.List<Order> getOrdersByUserId(Long userId, Integer status) {
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("create_time");
        return orderMapper.selectList(wrapper);
    }

    /**
     * 取消订单（仅待支付状态可取消，退回已扣余额）。
     * <p>使用 CAS 更新避免与支付回调的竞态：只有 status=0 才能改为 status=2。</p>
     */
    @Override
    @Transactional
    public String cancelOrder(Long userId, String orderNo) {
        Order order = getByOrderNo(orderNo);
        if (order == null) {
            return "操作失败：订单不存在";
        }
        if (!order.getUserId().equals(userId)) {
            return "操作失败：该订单不属于当前用户";
        }

        // CAS 取消：只有待支付(0)才能改为已取消(2)
        QueryWrapper<Order> casWrapper = new QueryWrapper<>();
        casWrapper.eq("order_no", orderNo).eq("status", 0);
        Order cancelUpdate = new Order();
        cancelUpdate.setStatus(2);
        cancelUpdate.setBalancePaid(0.0);
        int rows = orderMapper.update(cancelUpdate, casWrapper);
        if (rows == 0) {
            return "操作失败：订单已被支付或已取消";
        }

        // 退回已扣余额
        if (order.getBalancePaid() != null && order.getBalancePaid() > 0) {
            userService.refundBalance(userId, order.getBalancePaid());
            log.info("取消订单 {} 退回余额 ¥{} 给用户 {}", orderNo, order.getBalancePaid(), userId);
        }

        return "订单取消成功，订单号：" + orderNo;
    }

    /**
     * 使用余额支付待支付订单（余额优先扣款）
     */
    @Override
    @Transactional
    public String payOrderWithBalance(Long userId, String orderNo) {
        Order order = getByOrderNo(orderNo);
        if (order == null) {
            return "操作失败：订单不存在";
        }
        if (!order.getUserId().equals(userId)) {
            return "操作失败：该订单不属于当前用户";
        }
        if (order.getStatus() != 0) {
            return "操作失败：该订单不是待支付状态";
        }

        User user = userService.getById(userId);
        double balance = user.getBalance() == null ? 0 : user.getBalance();
        double totalAmount = order.getAmount();
        double alreadyPaid = order.getBalancePaid() == null ? 0 : order.getBalancePaid();
        double remaining = totalAmount - alreadyPaid;

        if (balance <= 0) {
            return "操作失败：余额为0，请先充值或使用其他支付方式";
        }

        if (balance >= remaining) {
            // 余额充足 → 扣除剩余金额 → 支付成功
            userService.deductBalance(userId, remaining);
            order.setBalancePaid(totalAmount);
            payOrder(orderNo, 0, "BALANCE");
            return String.format("✅ 余额支付成功！订单号：%s，扣款 ¥%.2f，余额剩余 ¥%.2f",
                    orderNo, remaining, balance - remaining);
        } else {
            // 余额不足 → 扣光余额 → 返回剩余需付金额
            userService.deductBalance(userId, balance);
            order.setBalancePaid(alreadyPaid + balance);
            orderMapper.updateById(order);
            double stillRemaining = remaining - balance;
            return String.format("⚠️ 余额不足，已扣 ¥%.2f，还需支付 ¥%.2f。请点击收银台完成支付：<a href='/pay?orderNo=%s' target='_blank'>去支付</a>",
                    balance, stillRemaining, orderNo);
        }
    }
}
