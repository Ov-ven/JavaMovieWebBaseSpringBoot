package com.software.movie.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("movie_person")
public class MoviePerson {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long movieId;
    private Long personId;
    private Integer roleType; // 0-演员 1-导演
    private String roleName;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}