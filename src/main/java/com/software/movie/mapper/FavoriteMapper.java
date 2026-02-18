package com.software.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.software.movie.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
}