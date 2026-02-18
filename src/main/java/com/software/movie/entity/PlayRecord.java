package com.software.movie.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("play_record")
public class PlayRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long movieId;
    private Date playTime;
    private Integer duration;
}