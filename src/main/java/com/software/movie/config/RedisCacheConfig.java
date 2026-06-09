package com.software.movie.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Redis 缓存与序列化配置类。
 * <p>负责注册以下 Bean：</p>
 * <ul>
 *   <li>{@link DefaultRedisScript} - 秒杀 Lua 脚本，保证原子性扣减库存</li>
 *   <li>{@link CacheManager} - Spring Cache 缓存管理器，默认 30 分钟过期</li>
 *   <li>{@link RedisTemplate} - Redis 操作模板，Key 使用 String 序列化，Value 使用 Jackson JSON 序列化</li>
 * </ul>
 */
@Configuration
public class RedisCacheConfig {

    /**
     * 注册秒杀 Lua 脚本 Bean。
     * <p>脚本位于 classpath:scripts/seckill.lua，用于 Redis 端原子执行库存扣减。</p>
     *
     * @return 秒杀 Lua 脚本对象
     */
    @Bean
    public DefaultRedisScript<Long> seckillScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setLocation(new ClassPathResource("scripts/seckill.lua"));
        redisScript.setResultType(Long.class);
        return redisScript;
    }

    /**
     * 配置 Spring Cache 缓存管理器。
     * <p>特性：</p>
     * <ul>
     *   <li>防雪崩：基础 TTL 30 分钟 + 随机 1~5 分钟偏移</li>
     *   <li>防穿透：允许缓存空值，避免对不存在的 Key 高频查询穿透到 DB</li>
     *   <li>序列化：Key 使用 String，Value 使用 Jackson JSON</li>
     * </ul>
     *
     * @param factory Redis 连接工厂
     * @return 配置好的 {@link CacheManager} 实例
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisSerializer<String> redisSerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);

        // 基础 TTL 30 分钟 + 随机 1~5 分钟偏移（防雪崩）
        long randomOffsetMinutes = ThreadLocalRandom.current().nextLong(1, 6);
        Duration ttl = Duration.ofMinutes(30 + randomOffsetMinutes);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer));
                // 允许缓存 null 值（防穿透：避免对不存在的 Key 高频查询穿透到 DB）

        RedisCacheManager cacheManager = RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .transactionAware()
                .build();
        return cacheManager;
    }

    /**
     * 配置 Redis 操作模板。
     * <p>Key 使用 {@link StringRedisSerializer} 序列化，
     * Value 使用 {@link Jackson2JsonRedisSerializer} 以 JSON 格式序列化，支持多态类型反序列化。</p>
     *
     * @param connectionFactory Redis 连接工厂
     * @return 配置好的 {@link RedisTemplate} 实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 使用 StringRedisSerializer 来序列化和反序列化 redis 的 key 值
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);

        // 使用 Jackson2JsonRedisSerializer 来序列化和反序列化 redis 的 value 值
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 确保能够序列化非final类型的类，例如你定义的实体类
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);

        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet(); // 初始化设置
        return template;
    }
}
