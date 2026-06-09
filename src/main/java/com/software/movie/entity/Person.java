package com.software.movie.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 影人实体类，映射 person 表。
 * 存储演员和导演的基本信息。
 */
@Data
@TableName("person")
public class Person {
    /** 影人ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 影人姓名 */
    private String name;
    /** 照片URL */
    private String photo;
    /** 影人类型（0-演员，1-导演） */
    private Integer type; // 0-演员 1-导演
    /** 影人简介 */
    private String description;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /** 更新时间，插入或更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
