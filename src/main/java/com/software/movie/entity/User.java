package com.software.movie.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 用户实体类，映射 user 表。
 * 存储用户的基本信息、VIP状态及登录凭证。
 */
@Data
@TableName("user")
public class User {
    /** 用户ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户名 */
    private String username;
    /** 密码（加密存储） */
    private String password;
    /** 邮箱 */
    private String email;
    /** 手机号 */
    private String phone;
    /** 头像URL */
    private String avatar;
    /** 账户余额 */
    private Double balance;

    /**
     * 获取是否为VIP用户。
     *
     * @return 1表示VIP用户，0表示普通用户
     */
    public int getIsvip() {
        return isvip;
    }

    /**
     * 设置是否为VIP用户。
     *
     * @param isvip 1表示VIP用户，0表示普通用户
     */
    public void setIsvip(int isvip) {
        this.isvip = isvip;
    }

    /** 是否VIP用户（1-VIP用户，0-普通用户） */
    @TableField("is_vip")
    private int isvip;

    /** VIP过期时间 */
    private Date vipExpireTime;
    /** 账号状态（true-正常，false-禁用） */
    private Boolean status;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /** 更新时间，插入或更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /** 登录令牌（非数据库字段，登录后填充） */
    @TableField(exist = false)
    private String token;

}