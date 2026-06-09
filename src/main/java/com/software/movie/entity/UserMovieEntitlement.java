package com.software.movie.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 用户电影资产凭证（已购凭证）。
 * <p>支付成功后插入，作为用户拥有某部电影的唯一凭证。
 * (user_id, movie_id) 联合唯一索引保证同一用户对同一电影只能购买一次。</p>
 */
@Data
@TableName("user_movie_entitlement")
public class UserMovieEntitlement {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 电影ID */
    private Long movieId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
