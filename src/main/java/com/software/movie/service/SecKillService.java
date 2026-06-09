package com.software.movie.service;

/**
 * 秒杀服务接口。
 * 提供基于 Redis + RocketMQ 事务消息的秒杀扣减能力。
 */
public interface SecKillService {

    /**
     * 执行秒杀扣减（Redis + Lua 原子操作）
     *
     * @param movieId 电影ID
     * @param userId  用户ID
     * @return true=抢购成功, false=售罄
     */
    boolean executeSeckill(Long movieId, Long userId);
}
