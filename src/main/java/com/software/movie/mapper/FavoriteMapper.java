package com.software.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.software.movie.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户收藏数据访问层接口。
 * 继承 MyBatis-Plus 的 BaseMapper，提供收藏记录的 CRUD 操作。
 */
@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
}