package com.software.movie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.software.movie.entity.Favorite;
import com.software.movie.entity.Movie;

import java.util.List;

/**
 * 收藏服务接口。
 * 提供用户对电影的收藏、取消收藏及收藏状态查询功能。
 */
public interface FavoriteService extends IService<Favorite> {

    /**
     * 添加收藏
     *
     * @param userId  用户ID
     * @param movieId 电影ID
     * @return 操作是否成功
     */
    boolean addFavorite(Long userId, Long movieId);

    /**
     * 取消收藏
     *
     * @param userId  用户ID
     * @param movieId 电影ID
     * @return 操作是否成功
     */
    boolean removeFavorite(Long userId, Long movieId);

    /**
     * 查询用户是否已收藏某电影
     *
     * @param userId  用户ID
     * @param movieId 电影ID
     * @return 是否已收藏
     */
    boolean isFavorite(Long userId, Long movieId);

    /**
     * 查询用户的收藏电影列表
     *
     * @param userId 用户ID
     * @return 收藏的电影列表
     */
    List<Movie> getFavoriteMovies(Long userId);
}
