package com.software.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.software.movie.entity.PlayRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PlayRecordMapper extends BaseMapper<PlayRecord> {
}