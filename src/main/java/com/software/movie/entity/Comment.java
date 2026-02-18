package com.software.movie.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;


@Data
@TableName("comment")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long movieId;
    private Long userId;
    private String content;
    private Double score;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    // 非数据库字段
    @TableField(exist = false)
    private String username;
    @TableField(exist = false)
    private String avatar;

}