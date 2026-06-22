package com.software.movie.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 电影实体类，映射 movie 表。
 * 存储电影的基本信息，包括标题、封面、简介、时长、评分、价格等。
 */
@Data
@TableName("movie")
public class Movie implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
    /** 电影ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 电影标题 */
    private String title;
    /** 封面图片URL */
    private String cover;
    /** 电影简介 */
    private String description;
    /** 上映日期 */
    private Date releaseDate;
    /** 时长（分钟） */
    private Integer duration;
    /** 上映地区 */
    private String region;
    /** 电影类型（如：动作、喜剧等） */
    private String type;
    /** 评分 */
    private Double score;
    /** 观看次数 */
    private Integer views;

    /**
     * 获取是否为VIP影片。
     *
     * @return 1表示VIP影片，0表示普通影片
     */
    public int getIsVip() {
        return isVip;
    }

    /**
     * 设置是否为VIP影片。
     *
     * @param isVip 1表示VIP影片，0表示普通影片
     */
    public void setIsVip(int isVip) {
        this.isVip = isVip;
    }

    /** 是否VIP影片（1-VIP影片，0-普通影片） */
    @TableField("is_vip")
    private int isVip;

    /** 价格（元） */
    private Double price;
    /** 秒杀价（元），null 表示不参与秒杀 */
    private Double seckillPrice;
    /** 秒杀库存，null 表示不参与秒杀 */
    private Integer seckillStock;

    /** 上架状态（true-上架，false-下架） */
    private Boolean status;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /** 更新时间，插入或更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /** 是否已收藏（非数据库字段，业务逻辑填充） */
    @TableField(exist = false)
    private Boolean isFavorite;
}