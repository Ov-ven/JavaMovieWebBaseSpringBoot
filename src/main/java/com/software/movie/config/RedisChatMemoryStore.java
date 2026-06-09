package com.software.movie.config;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的对话记忆持久化存储。
 * <p>使用 LangChain4j 官方序列化工具（ChatMessageSerializer/Deserializer）处理多态对象，
 * 以纯 JSON 字符串存入 Redis，避免手写 Jackson 多态配置。</p>
 */
@Component
public class RedisChatMemoryStore implements ChatMemoryStore {

    private static final Logger log = LoggerFactory.getLogger(RedisChatMemoryStore.class);

    /** Redis Key 前缀 */
    private static final String KEY_PREFIX = "movie:agent:memory:";

    /** 记忆过期时间（7天） */
    private static final long EXPIRE_DAYS = 7;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 从 Redis 获取用户的对话记忆。
     *
     * @param memoryId 用户ID（由 @MemoryId 注入）
     * @return 对话消息列表，不存在时返回空列表
     */
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String key = KEY_PREFIX + memoryId;
        String json = stringRedisTemplate.opsForValue().get(key);

        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            return ChatMessageDeserializer.messagesFromJson(json);
        } catch (Exception e) {
            log.error("反序列化对话记忆失败，memoryId={}", memoryId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 全量覆盖用户的对话记忆到 Redis。
     *
     * @param memoryId 用户ID
     * @param messages 最新的消息列表（由 LangChain4j 的 MessageWindowChatMemory 裁剪后传入）
     */
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String key = KEY_PREFIX + memoryId;

        if (messages == null || messages.isEmpty()) {
            stringRedisTemplate.delete(key);
            return;
        }

        try {
            String json = ChatMessageSerializer.messagesToJson(messages);
            stringRedisTemplate.opsForValue().set(key, json);
            // 强制刷新 TTL，确保即使 set 未带过期参数也能自动过期
            stringRedisTemplate.expire(key, EXPIRE_DAYS, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("序列化对话记忆失败，memoryId={}", memoryId, e);
        }
    }

    /**
     * 清除用户的对话记忆。
     *
     * @param memoryId 用户ID
     */
    @Override
    public void deleteMessages(Object memoryId) {
        String key = KEY_PREFIX + memoryId;
        stringRedisTemplate.delete(key);
    }
}
