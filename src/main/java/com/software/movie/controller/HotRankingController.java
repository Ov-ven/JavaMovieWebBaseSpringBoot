package com.software.movie.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.software.movie.entity.Movie;
import com.software.movie.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.Collections;
import java.util.List;

/**
 * 热播排行榜控制器
 * <p>
 * 负责展示热播电影排行榜页面，按播放量排序展示热门电影。
 * </p>
 */
@Controller
@RequestMapping("/rank") // 添加类级别的请求映射
public class HotRankingController {

    @Autowired
    private MovieService movieService;


    /**
     * 展示热播电影排行榜页面
     *
     * @param limit 显示的电影数量，默认 10，范围 1-100
     * @param model Spring MVC Model 对象，用于向视图传递数据
     * @return rank/hot 视图名称
     */
    @GetMapping("/hot")
    public String hotRanking(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer limit,
            Model model
    ) {
        List<Movie> movies = movieService.getHotMovies(limit);
        model.addAttribute("hotMovies", movies);

        // 为 header 模板添加占位变量
        addHeaderPlaceholders(model);
        
        return "rank/hot";
    }

    /**
     * 为 header 模板添加占位变量（避免 Thymeleaf 报错）
     */
    private void addHeaderPlaceholders(Model model) {
        model.addAttribute("queryType", null);
        model.addAttribute("queryRegion", null);
        model.addAttribute("queryKeyword", null);
        model.addAttribute("querySort", null);
        model.addAttribute("currentPage", 1);

        IPage<Movie> dummyMoviePage = new Page<>(1, 12);
        dummyMoviePage.setRecords(Collections.emptyList());
        dummyMoviePage.setTotal(0);
        model.addAttribute("moviePage", dummyMoviePage);
    }
}
