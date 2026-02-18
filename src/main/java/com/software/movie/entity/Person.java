package com.software.movie.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("person")
public class Person {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String photo;
    private Integer type; // 0-演员 1-导演
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}