package com.software.movie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.software.movie.entity.Favorite;
import com.software.movie.entity.Movie;
import com.software.movie.mapper.FavoriteMapper;
import com.software.movie.mapper.MovieMapper;
import com.software.movie.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 收藏服务实现类。
 * 提供用户对电影的收藏、取消收藏及收藏状态查询功能。
 */
@Service
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {
    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private MovieMapper movieMapper;

    /**
     * 添加收藏
     *
     * @param userId  用户ID
     * @param movieId 电影ID
     * @return 操作是否成功
     */
    @Override
    public boolean addFavorite(Long userId, Long movieId) {
        if (isFavorite(userId, movieId)) {
            return true; // 已收藏，幂等返回
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setMovieId(movieId);
        return favoriteMapper.insert(favorite) > 0;
    }

    /**
     * 取消收藏
     *
     * @param userId  用户ID
     * @param movieId 电影ID
     * @return 操作是否成功
     */
    @Override
    public boolean removeFavorite(Long userId, Long movieId) {
        QueryWrapper<Favorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("movie_id", movieId);
        return favoriteMapper.delete(wrapper) > 0;
    }

    /**
     * 查询用户是否已收藏某电影
     *
     * @param userId  用户ID
     * @param movieId 电影ID
     * @return 是否已收藏
     */
    @Override
    public boolean isFavorite(Long userId, Long movieId) {
        QueryWrapper<Favorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("movie_id", movieId);
        return favoriteMapper.selectCount(wrapper) > 0;
    }

    /**
     * 查询用户的收藏电影列表
     */
    @Override
    public List<Movie> getFavoriteMovies(Long userId) {
        QueryWrapper<Favorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("create_time");
        List<Favorite> favorites = favoriteMapper.selectList(wrapper);

        if (favorites.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> movieIds = favorites.stream()
                .map(Favorite::getMovieId)
                .collect(Collectors.toList());

        return movieMapper.selectBatchIds(movieIds);
    }
}
