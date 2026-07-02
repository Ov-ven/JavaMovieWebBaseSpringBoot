package com.software.movie.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.software.movie.entity.Faq;
import com.software.movie.mapper.FaqMapper;
import com.software.movie.service.FaqService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * FAQ服务实现类。
 * 提供FAQ的查询、关键词搜索和分类查询功能。
 */
@Service
public class FaqServiceImpl extends ServiceImpl<FaqMapper, Faq> implements FaqService {

    /**
     * 根据关键词搜索FAQ（模糊匹配问题和答案）
     *
     * @param keyword 搜索关键词
     * @param limit   返回数量限制
     * @return 匹配的FAQ列表
     */
    @Override
    public List<Faq> searchByKeyword(String keyword, int limit) {
        return baseMapper.searchByKeyword(keyword, limit);
    }

    /**
     * 根据分类查询FAQ
     *
     * @param category 分类名称
     * @return 该分类下的FAQ列表
     */
    @Override
    public List<Faq> getByCategory(String category) {
        return baseMapper.selectByCategory(category);
    }

    /**
     * 根据ID列表批量查询FAQ
     *
     * @param ids ID列表
     * @return FAQ列表
     */
    @Override
    public List<Faq> getByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return baseMapper.selectByIds(ids);
    }
}
