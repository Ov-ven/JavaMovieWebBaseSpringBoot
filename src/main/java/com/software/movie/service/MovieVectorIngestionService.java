package com.software.movie.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.software.movie.entity.Movie;
import com.software.movie.entity.dto.MovieQueryDTO;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 电影向量数据摄入服务。
 * <p>将电影数据向量化后存入 Redis RediSearch，供 RAG 检索增强生成使用。</p>
 */
@Service
public class MovieVectorIngestionService {

    private static final Logger log = LoggerFactory.getLogger(MovieVectorIngestionService.class);

    @Autowired
    private MovieService movieService;

    @Autowired(required = false)
    private EmbeddingModel embeddingModel;

    @Autowired(required = false)
    private EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    /**
     * 将所有电影数据向量化并存入 Redis。
     * <p>每部电影的名称、类型、地区、评分、简介拼装为一段文本，
     * 经 Embedding 模型生成向量后存入 Redis RediSearch。</p>
     *
     * @return 成功摄入的电影数量
     */
    public int ingestAllMoviesToVectorStore() {
        if (embeddingModel == null || embeddingStore == null) {
            log.warn("EmbeddingModel 或 EmbeddingStore 未就绪，跳过全量向量化");
            return 0;
        }
        log.info("开始电影向量化摄入...");

        // 1. 查询所有电影
        MovieQueryDTO queryDTO = new MovieQueryDTO();
        queryDTO.setPageSize(1000); // 一次取完
        IPage<Movie> page = movieService.getMoviePage(queryDTO);
        List<Movie> movies = page.getRecords();

        if (movies.isEmpty()) {
            log.warn("数据库中没有电影数据，跳过摄入");
            return 0;
        }

        int successCount = 0;

        for (Movie movie : movies) {
            try {
                // 2. 拼装电影文本
                String text = buildMovieText(movie);

                // 3. 构建 Metadata
                Metadata metadata = buildMetadata(movie);

                // 4. 构建 TextSegment
                TextSegment segment = TextSegment.from(text, metadata);

                // 5. 生成向量（embed() 返回 Response<Embedding>，需取 content）
                Response<Embedding> response = embeddingModel.embed(segment);
                Embedding embedding = response.content();

                // 6. 存入 Redis（使用 movieId 作为自定义 ID，Redis Key: movie_semantic_index:{movieId}）
                embeddingStore.add(String.valueOf(movie.getId()), embedding);

                successCount++;
                log.debug("电影 [{}] {} 向量化成功", movie.getId(), movie.getTitle());

            } catch (Exception e) {
                log.error("电影 [{}] {} 向量化失败: {}", movie.getId(), movie.getTitle(), e.getMessage());
            }
        }

        log.info("电影向量化摄入完成：成功 {}/{}", successCount, movies.size());
        return successCount;
    }

    /**
     * 将单部电影向量化并存入 Redis（用于新增/更新电影时实时摄入）。
     * <p>注意：langchain4j-redis 0.29.0 不支持按 metadata 删除旧向量，
     * 因此更新时会产生重复向量。语义搜索时会返回多条相似结果，
     * 上层调用方需做去重处理。</p>
     *
     * @param movieId 电影ID
     */
    public void ingestSingleMovie(Long movieId) {
        if (embeddingModel == null || embeddingStore == null) {
            log.warn("EmbeddingModel 或 EmbeddingStore 未就绪，跳过增量向量化 movieId={}", movieId);
            return;
        }
        Movie movie = movieService.getById(movieId);
        if (movie == null) {
            log.warn("电影 [{}] 不存在，跳过向量化", movieId);
            return;
        }
        try {
            String text = buildMovieText(movie);
            Metadata metadata = buildMetadata(movie);
            TextSegment segment = TextSegment.from(text, metadata);
            Response<Embedding> response = embeddingModel.embed(segment);
            Embedding embedding = response.content();
            // 使用 movieId 作为自定义 ID，Redis Key: movie_semantic_index:{movieId}
            embeddingStore.add(String.valueOf(movieId), embedding);
            log.info("电影 [{}] {} 增量向量化成功", movieId, movie.getTitle());
        } catch (Exception e) {
            log.error("电影 [{}] {} 增量向量化失败: {}", movieId, movie.getTitle(), e.getMessage());
        }
    }

    /**
     * 从向量库中精准移除指定电影。
     * <p>利用自定义 ID（movieId）直接删除 Redis 中对应的 Key，
     * Key 格式为 {indexName}:{movieId}，无需全量重建索引。</p>
     *
     * @param movieId 要移除的电影ID
     */
    public void removeSingleMovie(Long movieId) {
        try {
            // Redis Key 格式：movie_semantic_index:{movieId}
            String key = "movie_semantic_index:" + movieId;
            Boolean deleted = stringRedisTemplate.delete(key);
            log.info("电影 [{}] 向量删除结果：{}", movieId, deleted != null && deleted ? "成功" : "Key不存在");
        } catch (Exception e) {
            log.error("电影 [{}] 向量删除失败: {}", movieId, e.getMessage());
        }
    }

    /**
     * 构建电影的 Metadata。
     */
    private Metadata buildMetadata(Movie movie) {
        Metadata metadata = new Metadata();
        metadata.put("movieId", movie.getId().toString());
        metadata.put("title", movie.getTitle() != null ? movie.getTitle() : "");
        metadata.put("type", movie.getType() != null ? movie.getType() : "");
        metadata.put("region", movie.getRegion() != null ? movie.getRegion() : "");
        metadata.put("score", movie.getScore() != null ? movie.getScore().toString() : "0");
        metadata.put("isVip", String.valueOf(movie.getIsVip()));
        return metadata;
    }

    /**
     * 拼装电影的文本描述（用于向量化）。
     */
    private String buildMovieText(Movie movie) {
        StringBuilder sb = new StringBuilder();
        sb.append("电影名称：").append(movie.getTitle());
        if (movie.getType() != null) {
            sb.append("，类型：").append(movie.getType());
        }
        if (movie.getRegion() != null) {
            sb.append("，地区：").append(movie.getRegion());
        }
        if (movie.getScore() != null) {
            sb.append("，评分：").append(movie.getScore());
        }
        if (movie.getDuration() != null) {
            sb.append("，时长：").append(movie.getDuration()).append("分钟");
        }
        if (movie.getIsVip() == 1) {
            sb.append("，VIP专享");
        }
        if (movie.getDescription() != null && !movie.getDescription().isEmpty()) {
            sb.append("。剧情简介：").append(movie.getDescription());
        }
        return sb.toString();
    }
}
