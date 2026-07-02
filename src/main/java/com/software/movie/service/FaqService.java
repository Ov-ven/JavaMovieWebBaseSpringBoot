package com.software.movie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.software.movie.entity.Faq;

import java.util.List;

/**
 * FAQ服务接口。
 * 提供FAQ的查询、关键词搜索和分类查询功能。
 */
public interface FaqService extends IService<Faq> {

    /**
     * 根据关键词搜索FAQ（模糊匹配问题和答案）
     *
     * @param keyword 搜索关键词
     * @param limit   返回数量限制
     * @return 匹配的FAQ列表
     */
    List<Faq> searchByKeyword(String keyword, int limit);

    /**
     * 根据分类查询FAQ
     *
     * @param category 分类名称
     * @return 该分类下的FAQ列表
     */
    List<Faq> getByCategory(String category);

    /**
     * 根据ID列表批量查询FAQ
     *
     * @param ids ID列表
     * @return FAQ列表
     */
    List<Faq> getByIds(List<Long> ids);
}
