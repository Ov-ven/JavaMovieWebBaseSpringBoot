package com.software.movie.controller;

import com.software.movie.common.Result;
import com.software.movie.common.UserContext;
import com.software.movie.entity.Order;
import com.software.movie.entity.User;
import com.software.movie.service.OrderService;
import com.software.movie.service.SecKillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 秒杀活动控制器
 * <p>
 * 提供电影秒杀相关的 RESTful API，包括发起秒杀和轮询秒杀结果。
 * 秒杀服务依赖 RocketMQ，未启动时接口将返回错误提示。
 * </p>
 */
@RestController
@RequestMapping("/api/seckill")
public class SecKillController {

    @Autowired(required = false)
    private SecKillService secKillService;

    @Autowired
    private OrderService orderService;

    /**
     * 发起秒杀
     */
    @PostMapping("/doSeckill")
    public Result doSeckill(@RequestParam Long movieId) {
        if (secKillService == null) {
            return Result.error("秒杀服务未启用，请先启动 RocketMQ");
        }
        User user = UserContext.getUser();
        if (user == null) {
            return Result.error("请先登录");
        }
        boolean success = secKillService.executeSeckill(movieId, user.getId());
        if (success) {
            return Result.success("抢购成功，正在排队生成订单");
        }
        return Result.error("手慢了，已售罄");
    }

    /**
     * 轮询秒杀结果
     */
    @GetMapping("/result")
    public Result getResult(@RequestParam Long movieId) {
        User user = UserContext.getUser();
        if (user == null) {
            return Result.error("请先登录");
        }
        Order order = orderService.getByUserIdAndMovieId(user.getId(), movieId);
        if (order != null) {
            return Result.success(order.getOrderNo());
        }
        return Result.error(202, "正在排队处理中");
    }
}
