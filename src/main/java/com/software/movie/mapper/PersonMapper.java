package com.software.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.software.movie.entity.Person;
import org.apache.ibatis.annotations.Mapper;

/**
 * 影人数据访问层接口。
 * 继承 MyBatis-Plus 的 BaseMapper，提供影人信息的 CRUD 操作。
 */
@Mapper
public interface PersonMapper extends BaseMapper<Person> {
}