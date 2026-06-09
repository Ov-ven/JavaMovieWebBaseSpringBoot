package com.software.movie.controller;

import com.software.movie.common.Result;
import com.software.movie.common.UserContext;
import com.software.movie.entity.Movie;
import com.software.movie.entity.User;
import com.software.movie.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收藏管理控制器。
 * 提供收藏、取消收藏、查询收藏列表和检查收藏状态的 API。
 */
@RestController
@RequestMapping("/api/favorite")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    /**
     * 添加收藏
     */
    @PostMapping("/add")
    public Result addFavorite(@RequestParam Long movieId) {
        User user = UserContext.getUser();
        if (user == null) return Result.error("请先登录");

        boolean success = favoriteService.addFavorite(user.getId(), movieId);
        return success ? Result.success("收藏成功") : Result.error("收藏失败");
    }

    /**
     * 取消收藏
     */
    @PostMapping("/remove")
    public Result removeFavorite(@RequestParam Long movieId) {
        User user = UserContext.getUser();
        if (user == null) return Result.error("请先登录");

        boolean success = favoriteService.removeFavorite(user.getId(), movieId);
        return success ? Result.success("已取消收藏") : Result.error("取消失败");
    }

    /**
     * 查询当前用户的收藏列表
     */
    @GetMapping("/list")
    public Result getFavorites() {
        User user = UserContext.getUser();
        if (user == null) return Result.error("请先登录");

        List<Movie> movies = favoriteService.getFavoriteMovies(user.getId());
        return Result.success(movies);
    }

    /**
     * 检查是否已收藏某电影
     */
    @GetMapping("/check")
    public Result checkFavorite(@RequestParam Long movieId) {
        User user = UserContext.getUser();
        if (user == null) return Result.error("请先登录");

        boolean isFav = favoriteService.isFavorite(user.getId(), movieId);
        return Result.success(isFav);
    }
}
