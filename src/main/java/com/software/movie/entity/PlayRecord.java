package com.software.movie.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 播放记录实体类，映射 play_record 表。
 * 记录用户观看电影的播放历史，用于断点续播等功能。
 */
@Data
@TableName("play_record")
public class PlayRecord {
    /** 播放记录ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户ID */
    private Long userId;
    /** 电影ID */
    private Long movieId;
    /** 播放时间 */
    private Date playTime;
    /** 播放时长（秒） */
    private Integer duration;
}