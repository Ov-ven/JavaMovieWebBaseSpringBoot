package com.software.movie.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

/**
 * 电影评论实体类，映射 comment 表。
 * 记录用户对电影的评分和文字评论。
 */
@Data
@TableName("comment")
public class Comment {
    /** 评论ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 电影ID */
    private Long movieId;
    /** 评论用户ID */
    private Long userId;
    /** 评论内容 */
    private String content;
    /** 评分（0-10） */
    private Double score;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /** 评论用户名（非数据库字段，关联查询填充） */
    @TableField(exist = false)
    private String username;
    /** 评论用户头像（非数据库字段，关联查询填充） */
    @TableField(exist = false)
    private String avatar;

}