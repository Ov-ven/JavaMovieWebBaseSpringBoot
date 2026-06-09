package com.software.movie.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.redis.RedisEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * LangChain4j 模型配置。
 * <p>仅注册模型 Bean，MovieAssistant 由 ChatController 按请求动态构建（多实例状态隔离）。</p>
 */
@Configuration
public class LangChainConfig {

    private static final Logger log = LoggerFactory.getLogger(LangChainConfig.class);

    @Value("${llm.xiaomi.base-url}")
    private String baseUrl;

    @Value("${llm.xiaomi.api-key}")
    private String apiKey;

    @Value("${llm.xiaomi.model-name}")
    private String modelName;

    @Value("${llm.embedding.base-url}")
    private String embeddingBaseUrl;

    @Value("${llm.embedding.api-key}")
    private String embeddingApiKey;

    @Value("${llm.embedding.model-name}")
    private String embeddingModelName;

    @Bean
    public OpenAiChatModel openAiChatModel() {
        if (!StringUtils.hasText(apiKey)) {
            log.warn("MIMO_API_KEY 未配置，跳过 OpenAiChatModel 创建");
            return null;
        }
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .tokenizer(new OpenAiTokenizer("gpt-3.5-turbo"))
                .build();
    }

    @Bean
    public OpenAiStreamingChatModel openAiStreamingChatModel() {
        if (!StringUtils.hasText(apiKey)) {
            log.warn("MIMO_API_KEY 未配置，跳过 OpenAiStreamingChatModel 创建");
            return null;
        }
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .tokenizer(new OpenAiTokenizer("gpt-3.5-turbo"))
                .build();
    }

    /**
     * 聊天记忆提供者（按用户ID隔离会话记忆，保留最近20条消息，持久化到 Redis）。
     */
    @Bean
    public ChatMemoryProvider chatMemoryProvider(RedisChatMemoryStore redisChatMemoryStore) {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(20)
                .chatMemoryStore(redisChatMemoryStore)
                .build();
    }

    /**
     * 文本向量化模型（阿里云 DashScope - text-embedding-v4）。
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        if (!StringUtils.hasText(embeddingApiKey)) {
            log.warn("Llm_Embedding_Apikey 未配置，跳过 EmbeddingModel 创建");
            return null;
        }
        return OpenAiEmbeddingModel.builder()
                .baseUrl(embeddingBaseUrl)
                .apiKey(embeddingApiKey)
                .modelName(embeddingModelName)
                .build();
    }

    /**
     * Redis 向量存储（基于 RediSearch）。
     * <p>注意：dimension 必须与 Embedding 模型输出的向量维度一致。
     * text-embedding-v4 输出维度为 1024，若更换模型需同步调整。</p>
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        if (!StringUtils.hasText(embeddingApiKey)) {
            log.warn("Llm_Embedding_Apikey 未配置，跳过 EmbeddingStore 创建");
            return null;
        }
        try {
            // Redis Key 格式：{indexName}:{自定义ID}，如 movie_semantic_index:123
            return RedisEmbeddingStore.builder()
                    .host("localhost")
                    .port(6380)
                    .indexName("movie_semantic_index")
                    .dimension(1024)
                    .build();
        } catch (Exception e) {
            log.warn("Redis Stack 连接失败（端口 16380），跳过 EmbeddingStore 创建：{}", e.getMessage());
            return null;
        }
    }
}
