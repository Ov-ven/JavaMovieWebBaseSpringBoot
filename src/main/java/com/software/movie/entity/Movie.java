package com.software.movie.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("movie")
public class Movie {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String cover;
    private String description;
    private Date releaseDate;
    private Integer duration;
    private String region;
    private String type;
    private Double score;
    private Integer views;

    public int getIsVip() {
        return isVip;
    }

    public void setIsVip(int isVip) {
        this.isVip = isVip;
    }

    @TableField("is_vip")
    private int isVip;


    private Boolean status;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    // 非数据库字段
    @TableField(exist = false)
    private Boolean isFavorite;
}