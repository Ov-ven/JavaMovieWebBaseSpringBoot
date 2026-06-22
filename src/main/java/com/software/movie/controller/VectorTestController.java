package com.software.movie.controller;

import com.software.movie.common.Result;
import com.software.movie.common.event.MovieDataChangeEvent;
import com.software.movie.entity.Movie;
import com.software.movie.service.MovieService;
import com.software.movie.service.MovieVectorIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 向量同步性能对比测试控制器（无鉴权）。
 * <p>对比新增电影后，同步向量 vs 异步（MQ）向量的接口响应耗时差异。</p>
 */
@RestController
@RequestMapping("/test/vector")
public class VectorTestController {

    private static final Logger log = LoggerFactory.getLogger(VectorTestController.class);

    @Autowired
    private MovieService movieService;

    @Autowired(required = false)
    private MovieVectorIngestionService ingestionService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * 同步模式：新增电影 + 直接调 Embedding API 生成向量（阻塞）。
     * <p>接口耗时 = MySQL 写入 + DashScope API 调用 + Redis 写入。</p>
     */
    @PostMapping("/sync")
    public Result sync(@RequestParam(defaultValue = "test") String title) {
        long start = System.currentTimeMillis();
        try {
            // 1. 新增电影
            Movie movie = new Movie();
            movie.setTitle(title);
            movie.setType("测试");
            movie.setRegion("测试");
            movie.setScore(0.0);
            movie.setPrice(0.0);
            movie.setIsVip(0);
            movie.setStatus(true);
            movieService.save(movie);
            long dbCost = System.currentTimeMillis() - start;

            // 2. 同步生成向量（阻塞等待 DashScope API）
            long vectorStart = System.currentTimeMillis();
            if (ingestionService != null) {
                ingestionService.ingestSingleMovie(movie.getId());
            }
            long vectorCost = System.currentTimeMillis() - vectorStart;

            long totalCost = System.currentTimeMillis() - start;
            log.info("同步模式完成: movieId={}, DB={}ms, 向量={}ms, 总计={}ms",
                    movie.getId(), dbCost, vectorCost, totalCost);

            return Result.success("同步模式完成", Map.of(
                    "movieId", movie.getId(),
                    "mode", "sync",
                    "dbCostMs", dbCost,
                    "vectorCostMs", vectorCost,
                    "totalCostMs", totalCost
            ));
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            return Result.error("同步模式失败，耗时" + cost + "ms: " + e.getMessage());
        }
    }

    /**
     * 异步模式：新增电影 + 发布事件到 MQ（非阻塞）。
     * <p>接口耗时 = MySQL 写入 + 事件发布（几毫秒），向量同步在 MQ Consumer 后台执行。</p>
     */
    @PostMapping("/async")
    public Result async(@RequestParam(defaultValue = "test") String title) {
        long start = System.currentTimeMillis();
        try {
            // 1. 新增电影（save 内部会自动发布 MovieDataChangeEvent）
            Movie movie = new Movie();
            movie.setTitle(title);
            movie.setType("测试");
            movie.setRegion("测试");
            movie.setScore(0.0);
            movie.setPrice(0.0);
            movie.setIsVip(0);
            movie.setStatus(true);
            movieService.save(movie);

            long totalCost = System.currentTimeMillis() - start;
            log.info("异步模式完成: movieId={}, 接口耗时={}ms（向量同步在后台执行）",
                    movie.getId(), totalCost);

            return Result.success("异步模式完成，向量同步在后台执行", Map.of(
                    "movieId", movie.getId(),
                    "mode", "async",
                    "totalCostMs", totalCost
            ));
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            return Result.error("异步模式失败，耗时" + cost + "ms: " + e.getMessage());
        }
    }
}
