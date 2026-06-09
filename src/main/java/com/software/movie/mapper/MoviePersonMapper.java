package com.software.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.software.movie.entity.MoviePerson;
import org.apache.ibatis.annotations.Mapper;

/**
 * 电影-影人关联数据访问层接口。
 * 继承 MyBatis-Plus 的 BaseMapper，提供电影与影人关联记录的 CRUD 操作。
 */
@Mapper
public interface MoviePersonMapper extends BaseMapper<MoviePerson> {
}