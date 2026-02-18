package com.software.movie.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.software.movie.entity.Movie;
import com.software.movie.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/rank")
public class TopRankingController {

    private final MovieService movieService;

    @Autowired
    public TopRankingController(MovieService movieService) {
        this.movieService = movieService;
    }

    /**
     * 高分电影排行榜
     *
     * @param limit 显示的电影数量（默认20）
     * @param model 模型对象，用于传递数据到视图
     * @return 视图名称
     */
    @GetMapping("/top")
    public String topRanking(@RequestParam(defaultValue = "20") Integer limit, Model model) {
        // 获取按评分排序的高分电影列表
        List<Movie> topMovies = movieService.getTopScoreMovies(limit);

        // 将电影列表添加到模型
        model.addAttribute("topMovies", topMovies);

        // --- 这里添加了所有占位符 ---
        model.addAttribute("queryType", null);
        model.addAttribute("queryRegion", null);
        model.addAttribute("queryKeyword", null);
        model.addAttribute("querySort", null);
        model.addAttribute("currentPage", 1);

        IPage<Movie> dummyMoviePage = new Page<>(1, 12);
        dummyMoviePage.setRecords(Collections.emptyList());
        dummyMoviePage.setTotal(0);
        model.addAttribute("moviePage", dummyMoviePage);

        // 返回视图路径
        return "rank/top";
    }
}
