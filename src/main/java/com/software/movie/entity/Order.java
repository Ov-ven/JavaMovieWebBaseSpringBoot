package com.software.movie.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 订单实体类，映射 order_info 表。
 * 记录用户的电影购买订单和VIP购买订单信息。
 */
@Data
@TableName("order_info")
public class Order {
    /** 订单ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 订单编号（UUID） */
    private String orderNo;
    /** 用户ID */
    private Long userId;
    /** 电影ID（VIP购买时可为null） */
    private Long movieId;
    /** 订单金额（元） */
    private Double amount;
    /** 已通过余额支付的金额（混合支付时使用） */
    private Double balancePaid;
    /** 订单类型（1-单片购买，2-VIP购买） */
    private Integer orderType; // 1-单片购买 2-VIP购买
    /** 订单状态（0-未支付，1-已支付，2-已取消） */
    private Integer status; // 0-未支付 1-已支付 2-已取消
    /** 支付时间 */
    private Date payTime;
    /** 支付方式（1-支付宝，2-微信） */
    private Integer payType; // 1-支付宝 2-微信
    /** 第三方支付流水号 */
    private String payNo;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /** 更新时间，插入或更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}