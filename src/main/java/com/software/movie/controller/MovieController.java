package com.software.movie.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;

import com.software.movie.common.Result;
import com.software.movie.entity.Movie;
import com.software.movie.entity.User;
import com.software.movie.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;
@EnableCaching
@RestController
@RequestMapping("/api/movie")
public class MovieController {
    @Autowired
    private MovieService movieService;




    @GetMapping("/list")
    public Result getMovieList(@RequestParam(defaultValue = "1") Integer pageNum,
                               @RequestParam(defaultValue = "10") Integer pageSize,
                               @RequestParam(required = false) String type,
                               @RequestParam(required = false) String region,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(required = false) String sort,
                               Model model) {
        IPage<Movie> page = movieService.getMoviePage(pageNum, pageSize, type, region, keyword,sort);
        return Result.success(page);
    }

    @GetMapping("/hot")
    public Result getHotMovies(@RequestParam(defaultValue = "10") Integer limit) {
        List<Movie> movies = movieService.getHotMovies(limit);
        return Result.success(movies);
    }

    @GetMapping("/top")
    public Result getTopScoreMovies(@RequestParam(defaultValue = "10") Integer limit) {
        List<Movie> movies = movieService.getTopScoreMovies(limit);
        return Result.success(movies);
    }



    @GetMapping("/byPerson/{personId}")
    public Result getMoviesByPerson(@PathVariable Long personId) {
        List<Movie> movies = movieService.getMoviesByPerson(personId);
        return Result.success(movies);
    }

    @GetMapping("/monthlyTop")
    public Result getMonthlyTopMovies(@RequestParam(defaultValue = "10") Integer limit) {
        List<Movie> movies = movieService.getMonthlyTopMovies(limit);
        return Result.success(movies);
    }

    @GetMapping("/allTimeTop")
    public Result getAllTimeTopMovies(@RequestParam(defaultValue = "10") Integer limit) {
        List<Movie> movies = movieService.getAllTimeTopMovies(limit);
        return Result.success(movies);
    }

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
        if (movie.getIsVip()==1 && user.getIsvip()!=1) {
            return Result.error("此影片为vip影片，请升级为vip会员观看");
        }
        // 这里可以添加播放逻辑，如返回播放地址等
        return Result.success("可以播放");
    }

}