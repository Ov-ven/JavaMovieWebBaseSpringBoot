package com.software.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.software.movie.entity.MoviePerson;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MoviePersonMapper extends BaseMapper<MoviePerson> {
}