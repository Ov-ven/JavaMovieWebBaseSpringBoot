package com.software.movie.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.software.movie.common.Result; // 确保 Result.java 存在
import com.software.movie.entity.Movie;
import com.software.movie.service.MovieService; // 注入 MovieService


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap; // 用于保持排序后Map的顺序
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/charts")
public class ChartPageController {

    @Autowired
    private MovieService movieService; // 注入 MovieService
    private static final Logger logger = LoggerFactory.getLogger(ChartPageController.class);
    @GetMapping
    public String showChartsPage(Model model) {
        // --- 为 header 片段添加的占位符（如果需要，请保留）---
        model.addAttribute("queryType", null);
        model.addAttribute("queryRegion", null);
        model.addAttribute("queryKeyword", null);
        model.addAttribute("querySort", null);
        model.addAttribute("currentPage", 1);

        IPage<Movie> dummyMoviePage = new Page<>(1, 12);
        dummyMoviePage.setRecords(Collections.emptyList());
        dummyMoviePage.setTotal(0);
        model.addAttribute("moviePage", dummyMoviePage);
        // --- 占位符结束 ---

        // --- 将 ChartDataController 的逻辑移到这里 ---
        List<Movie> allMovies = movieService.getAllMovies();
        logger.info("获取到的所有电影数量: {}", allMovies.size()); // 打印所有电影数量
        // 电影类型播放量分布数据 (饼图)
        Map<String, Long> typeViews = allMovies.stream()
                .filter(movie -> movie.getType() != null && !movie.getType().isEmpty())
                .collect(Collectors.groupingBy(
                        Movie::getType,
                        Collectors.summingLong(Movie::getViews)
                ));
        List<Map<String, Object>> movieTypeChartData = typeViews.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", entry.getKey());
                    item.put("value", entry.getValue());
                    return item;
                })
                .sorted(Comparator.<Map<String, Object>, Long>comparing(map -> (Long) map.get("value"), Comparator.reverseOrder()))
                .collect(Collectors.toList());
        model.addAttribute("movieTypeChartData", movieTypeChartData); // 将数据添加到 Model
        logger.info("电影类型图表数据: {}", movieTypeChartData);
        // Top 10 电影播放量对比数据 (柱状图)
        List<Movie> topMovies = movieService.getHotMovies(10);
        logger.info("获取到的Top 10电影数量: {}", topMovies.size());
        List<String> titles = topMovies.stream()
                .map(Movie::getTitle)
                .collect(Collectors.toList());
        List<Integer> views = topMovies.stream()
                .map(Movie::getViews)
                .collect(Collectors.toList());
        Map<String, List<?>> topMoviesChartData = new LinkedHashMap<>();
        topMoviesChartData.put("titles", titles);
        topMoviesChartData.put("views", views);
        model.addAttribute("topMoviesChartData", topMoviesChartData); // 将数据添加到 Model
        // --- 逻辑结束 ---
        logger.info("Top 10电影图表数据: {}", topMoviesChartData);
        return "charts"; // 返回 templates/charts.html 视图
    }
}