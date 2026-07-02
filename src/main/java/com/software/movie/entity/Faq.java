package com.software.movie.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * FAQ实体类，映射 faq 表。
 * 存储常见问题和答案，用于AI助手的知识库问答。
 */
@Data
@TableName("faq")
public class Faq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** FAQ ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 问题 */
    private String question;

    /** 答案 */
    private String answer;

    /** 分类（如：账号注册、VIP会员、订单问题等） */
    private String category;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
