package com.software.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.software.movie.entity.Person;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PersonMapper extends BaseMapper<Person> {
}