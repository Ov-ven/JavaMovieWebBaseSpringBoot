package com.software.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.software.movie.entity.Faq;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * FAQ数据访问层接口。
 * 继承 MyBatis-Plus 的 BaseMapper，提供 FAQ 的 CRUD 及自定义查询方法。
 */
@Mapper
public interface FaqMapper extends BaseMapper<Faq> {

    /**
     * 根据关键词搜索FAQ（问题和答案模糊匹配）
     *
     * @param keyword 搜索关键词
     * @param limit   返回数量限制
     * @return 匹配的FAQ列表
     */
    @Select("SELECT * FROM faq WHERE question LIKE CONCAT('%', #{keyword}, '%') " +
            "OR answer LIKE CONCAT('%', #{keyword}, '%') LIMIT #{limit}")
    List<Faq> searchByKeyword(@Param("keyword") String keyword, @Param("limit") Integer limit);

    /**
     * 根据分类查询FAQ
     *
     * @param category 分类名称
     * @return 该分类下的FAQ列表
     */
    @Select("SELECT * FROM faq WHERE category = #{category}")
    List<Faq> selectByCategory(@Param("category") String category);

    /**
     * 根据ID列表批量查询FAQ
     *
     * @param ids ID列表
     * @return FAQ列表
     */
    @Select("<script>" +
            "SELECT * FROM faq WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<Faq> selectByIds(@Param("ids") List<Long> ids);
}
