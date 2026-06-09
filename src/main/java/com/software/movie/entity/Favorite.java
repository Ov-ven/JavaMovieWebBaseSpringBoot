package com.software.movie.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 用户收藏实体类，映射 favorite 表。
 * 记录用户收藏的电影关系。
 */
@Data
@TableName("favorite")
public class Favorite {
    /** 收藏记录ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户ID */
    private Long userId;
    /** 电影ID */
    private Long movieId;

    /** 收藏时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}