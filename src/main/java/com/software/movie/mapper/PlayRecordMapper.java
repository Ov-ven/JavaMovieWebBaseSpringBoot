package com.software.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.software.movie.entity.PlayRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 播放记录数据访问层接口。
 * 继承 MyBatis-Plus 的 BaseMapper，提供播放记录的 CRUD 操作。
 */
@Mapper
public interface PlayRecordMapper extends BaseMapper<PlayRecord> {
}