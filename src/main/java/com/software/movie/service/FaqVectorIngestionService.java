package com.software.movie.service;

import com.software.movie.entity.Faq;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * FAQ向量数据摄入服务。
 * <p>将FAQ数据向量化后存入 Redis RediSearch，供 AI 助手知识库问答使用。</p>
 */
@Service
public class FaqVectorIngestionService {

    private static final Logger log = LoggerFactory.getLogger(FaqVectorIngestionService.class);

    @Autowired
    private FaqService faqService;

    @Autowired(required = false)
    private EmbeddingModel embeddingModel;

    @Autowired(required = false)
    private EmbeddingStore<TextSegment> embeddingStore;

    /**
     * 将所有FAQ数据向量化并存入 Redis。
     * <p>每条FAQ的问题文本经 Embedding 模型生成向量后存入 Redis RediSearch。</p>
     *
     * @return 成功摄入的FAQ数量
     */
    public int ingestAllFaqToVectorStore() {
        if (embeddingModel == null || embeddingStore == null) {
            log.warn("EmbeddingModel 或 EmbeddingStore 未就绪，跳过FAQ全量向量化");
            return 0;
        }
        log.info("开始FAQ向量化摄入...");

        // 1. 查询所有FAQ
        List<Faq> faqList = faqService.list();
        log.info("从数据库查询到 {} 条FAQ", faqList.size());

        if (faqList.isEmpty()) {
            log.warn("数据库中没有FAQ数据，跳过摄入");
            return 0;
        }

        int successCount = 0;

        for (Faq faq : faqList) {
            try {
                log.info("正在处理FAQ [{}]: {}", faq.getId(), faq.getQuestion());

                // 2. 构建 Metadata
                Metadata metadata = new Metadata();
                metadata.put("faqId", faq.getId().toString());
                metadata.put("category", faq.getCategory() != null ? faq.getCategory() : "");

                // 3. 构建 TextSegment（使用问题文本进行向量化）
                TextSegment segment = TextSegment.from(faq.getQuestion(), metadata);

                // 4. 生成向量
                Response<Embedding> response = embeddingModel.embed(segment);
                Embedding embedding = response.content();
                log.debug("FAQ [{}] 向量生成成功，维度: {}", faq.getId(), embedding.vector().length);

                // 5. 存入 Redis（使用 faqId 作为自定义 ID）
                embeddingStore.add("faq:" + faq.getId(), embedding);

                successCount++;
                log.info("FAQ [{}] {} 向量化成功", faq.getId(), faq.getQuestion());

            } catch (Exception e) {
                log.error("FAQ [{}] {} 向量化失败: {}", faq.getId(), faq.getQuestion(), e.getMessage(), e);
            }
        }

        log.info("FAQ向量化摄入完成：成功 {}/{}", successCount, faqList.size());
        return successCount;
    }

    /**
     * 根据用户问题搜索相似的FAQ（向量相似度匹配）
     *
     * @param userQuestion 用户问题
     * @param maxResults   最大返回数量
     * @return 相似度最高的FAQ列表
     */
    public List<Faq> searchSimilarFaq(String userQuestion, int maxResults) {
        if (embeddingModel == null || embeddingStore == null) {
            log.warn("EmbeddingModel 或 EmbeddingStore 未就绪，跳过FAQ向量搜索");
            return List.of();
        }

        try {
            // 1. 将用户问题向量化
            Response<Embedding> response = embeddingModel.embed(userQuestion);
            Embedding embedding = response.content();

            // 2. 在向量库中搜索相似向量
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(embedding, maxResults);

            if (matches == null || matches.isEmpty()) {
                return List.of();
            }

            // 3. 从匹配结果中提取FAQ ID
            List<Long> faqIds = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> match : matches) {
                String embeddingId = match.embeddingId();
                if (embeddingId != null && embeddingId.startsWith("faq:")) {
                    try {
                        Long faqId = Long.parseLong(embeddingId.substring(4));
                        faqIds.add(faqId);
                    } catch (NumberFormatException e) {
                        log.warn("无法解析FAQ ID: {}", embeddingId);
                    }
                }
            }

            if (faqIds.isEmpty()) {
                return List.of();
            }

            // 4. 根据ID列表查询FAQ详情
            return faqService.getByIds(faqIds);

        } catch (Exception e) {
            log.error("FAQ向量搜索失败: {}", e.getMessage());
            return List.of();
        }
    }
}
