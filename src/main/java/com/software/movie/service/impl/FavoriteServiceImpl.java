package com.software.movie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.software.movie.entity.Favorite;
import com.software.movie.mapper.FavoriteMapper;
import com.software.movie.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {
    @Autowired
    private FavoriteMapper favoriteMapper;

    @Override
    public boolean addFavorite(Long userId, Long movieId) {
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setMovieId(movieId);
        return favoriteMapper.insert(favorite) > 0;
    }

    @Override
    public boolean removeFavorite(Long userId, Long movieId) {
        QueryWrapper<Favorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("movie_id", movieId);
        return favoriteMapper.delete(wrapper) > 0;
    }

    @Override
    public boolean isFavorite(Long userId, Long movieId) {
        QueryWrapper<Favorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("movie_id", movieId);
        return favoriteMapper.selectCount(wrapper) > 0;
    }
}