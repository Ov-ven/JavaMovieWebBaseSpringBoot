package com.software.movie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.software.movie.entity.Favorite;

public interface FavoriteService extends IService<Favorite> {
    boolean addFavorite(Long userId, Long movieId);

    boolean removeFavorite(Long userId, Long movieId);

    boolean isFavorite(Long userId, Long movieId);
}