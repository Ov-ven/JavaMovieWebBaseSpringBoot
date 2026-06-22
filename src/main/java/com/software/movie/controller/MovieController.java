package com.software.movie.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import com.software.movie.common.Result;
import com.software.movie.entity.Movie;
import com.software.movie.entity.User;
import com.software.movie.entity.UserMovieEntitlement;
import com.software.movie.entity.dto.MovieQueryDTO;
import com.software.movie.mapper.UserMovieEntitlementMapper;
import com.software.movie.service.MovieService;
import com.software.movie.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;
/**
 * 电影数据接口控制器
 * <p>
 * 提供电影相关的 RESTful API，包括电影列表查询、热门电影、高分电影、
 * 按主创人员查询、月度/全站排行以及播放权限校验等功能。
 * </p>
 */
@RestController
@RequestMapping("/api/movie")
public class MovieController {
    @Autowired
    private MovieService movieService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserMovieEntitlementMapper entitlementMapper;




    /**
     * 获取电影列表（分页、筛选、排序）
     *
     * @param queryDTO 查询参数封装
     * @return 包含分页电影数据的统一响应结果
     */
    @GetMapping("/list")
    public Result getMovieList(MovieQueryDTO queryDTO) {
        IPage<Movie> page = movieService.getMoviePage(queryDTO);
        return Result.success(page);
    }

    /**
     * 获取热门电影列表
     *
     * @param limit 返回的电影数量，默认 10
     * @return 包含热门电影列表的统一响应结果
     */
    @GetMapping("/hot")
    public Result getHotMovies(@RequestParam(defaultValue = "10") Integer limit) {
        List<Movie> movies = movieService.getHotMovies(limit);
        return Result.success(movies);
    }

    /**
     * 获取高分电影列表
     *
     * @param limit 返回的电影数量，默认 10
     * @return 包含高分电影列表的统一响应结果
     */
    @GetMapping("/top")
    public Result getTopScoreMovies(@RequestParam(defaultValue = "10") Integer limit) {
        List<Movie> movies = movieService.getTopScoreMovies(limit);
        return Result.success(movies);
    }



    /**
     * 根据主创人员 ID 获取相关电影列表
     *
     * @param personId 主创人员 ID
     * @return 包含该主创参与的电影列表的统一响应结果
     */
    @GetMapping("/byPerson/{personId}")
    public Result getMoviesByPerson(@PathVariable Long personId) {
        List<Movie> movies = movieService.getMoviesByPerson(personId);
        return Result.success(movies);
    }

    /**
     * 获取月度热门电影排行榜
     *
     * @param limit 返回的电影数量，默认 10
     * @return 包含月度热门电影列表的统一响应结果
     */
    @GetMapping("/monthlyTop")
    public Result getMonthlyTopMovies(@RequestParam(defaultValue = "10") Integer limit) {
        List<Movie> movies = movieService.getMonthlyTopMovies(limit);
        return Result.success(movies);
    }

    /**
     * 获取全站历史热门电影排行榜
     *
     * @param limit 返回的电影数量，默认 10
     * @return 包含全站热门电影列表的统一响应结果
     */
    @GetMapping("/allTimeTop")
    public Result getAllTimeTopMovies(@RequestParam(defaultValue = "10") Integer limit) {
        List<Movie> movies = movieService.getAllTimeTopMovies(limit);
        return Result.success(movies);
    }

    /**
     * 校验电影播放权限
     * <p>
     * 检查用户是否登录以及是否有权限观看指定电影（VIP 影片需要 VIP 用户）。
     * 通过查询 user_movie_entitlement 凭证表判断是否已购买。
     * </p>
     *
     * @param movieId 电影 ID
     * @param session HTTP 会话，用于获取当前登录用户信息
     * @return 校验通过返回成功提示，否则返回错误原因
     */
    @GetMapping("/play/{movieId}")
    public Result playMovie(@PathVariable Long movieId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error("请先登录");
        }
        Movie movie = movieService.getMovieDetail(movieId);
        if (movie == null) {
            return Result.error("电影不存在");
        }
        boolean isVipMovie = Integer.valueOf(1).equals(movie.getIsVip());
        boolean isUserVip = userService.isVip(user.getId());
        boolean hasPrice = movie.getPrice() != null && movie.getPrice() > 0;

        if (isVipMovie && !isUserVip) {
            // 非VIP用户看VIP电影 → 必须有购买凭证
            if (!hasEntitlement(user.getId(), movieId)) {
                return Result.error("此影片为VIP专享，请购买后观看");
            }
        } else if (!isVipMovie && hasPrice) {
            // 非VIP电影但有价格 → 必须有购买凭证
            if (!hasEntitlement(user.getId(), movieId)) {
                return Result.error("此影片需购买后观看");
            }
        }
        return Result.success("可以播放");
    }

    /**
     * 检查用户是否拥有某部电影的观看凭证
     */
    private boolean hasEntitlement(Long userId, Long movieId) {
        QueryWrapper<UserMovieEntitlement> ew = new QueryWrapper<>();
        ew.eq("user_id", userId).eq("movie_id", movieId);
        return entitlementMapper.selectCount(ew) > 0;
    }

}