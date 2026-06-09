package com.software.movie.entity.dto;

import lombok.Data;

/**
 * 电影查询参数封装。
 * <p>用于统一电影列表查询的入参，替代多参数方法签名，
 * 同时为大模型 Function Calling 提供结构化的参数定义。</p>
 */
@Data
public class MovieQueryDTO {

    /** 页码，默认 1 */
    private Integer pageNum = 1;

    /** 每页数量，默认 12 */
    private Integer pageSize = 12;

    /** 电影类型，如：科幻、动作、喜剧、剧情 */
    private String type;

    /** 电影地区，如：美国、中国大陆、日本 */
    private String region;

    /** 关键词搜索（模糊匹配电影标题和描述） */
    private String keyword;

    /** 排序方式：hot=最热, top=最高分, new=最新 */
    private String sort;

    /** 是否只查 VIP 电影（1=是，null=不限） */
    private Integer isVip;

    /** 是否只查免费电影（1=是，null=不限） */
    private Integer free;
}
