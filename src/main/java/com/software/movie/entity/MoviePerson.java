package com.software.movie.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 电影-影人关联实体类，映射 movie_person 表。
 * 记录电影与演员/导演之间的多对多关系。
 */
@Data
@TableName("movie_person")
public class MoviePerson {
    /** 关联记录ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 电影ID */
    private Long movieId;
    /** 影人ID */
    private Long personId;
    /** 角色类型（0-演员，1-导演） */
    private Integer roleType; // 0-演员 1-导演
    /** 角色名称 */
    private String roleName;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
