package com.software.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.software.movie.entity.Movie;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 电影数据访问层接口。
 * 继承 MyBatis-Plus 的 BaseMapper，提供电影的 CRUD 及自定义查询方法。
 */
@Mapper
public interface MovieMapper extends BaseMapper<Movie> {
    /**
     * 条件分页查询电影。
     * 支持按类型、地区和关键词（标题或简介）模糊筛选。
     *
     * @param page    分页参数
     * @param type    电影类型（可为null）
     * @param region  上映地区（可为null）
     * @param keyword 搜索关键词（可为null）
     * @return 电影分页结果
     */
    @Select("<script>" +
            "SELECT * FROM movie " +
            "WHERE status = 1 " +
            "<if test=\"type != null and type != ''\">AND type LIKE CONCAT('%', #{type}, '%')</if> " +
            "<if test=\"region != null and region != ''\">AND region LIKE CONCAT('%', #{region}, '%')</if> " +
            "<if test=\"keyword != null and keyword != ''\">" +
            "AND (title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR description LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if> " +
            "ORDER BY release_date DESC" +
            "</script>")
    IPage<Movie> selectPageByCondition(Page<Movie> page,
                                       @Param("type") String type,
                                       @Param("region") String region,
                                       @Param("keyword") String keyword);

    /**
     * 获取热门电影列表，按观看次数降序排序。
     *
     * @param limit 返回数量限制
     * @return 热门电影列表
     */
    @Select("SELECT * FROM movie WHERE status = 1 ORDER BY views DESC LIMIT #{limit}")
    List<Movie> selectHotMovies(@Param("limit") Integer limit);

    /**
     * 获取高分电影列表，按评分降序排序。
     *
     * @param limit 返回数量限制
     * @return 高分电影列表
     */
    @Select("SELECT * FROM movie WHERE status = 1 AND score > 0 ORDER BY score DESC LIMIT #{limit}")
    List<Movie> selectTopScoreMovies(@Param("limit") Integer limit);

    /**
     * 根据影人ID查询其参与的所有上架电影。
     *
     * @param personId 影人ID
     * @return 相关电影列表，按上映日期降序排序
     */
    @Select("SELECT m.* FROM movie m " +
            "JOIN movie_person mp ON m.id = mp.movie_id " +
            "WHERE mp.person_id = #{personId} AND m.status = 1 " +
            "ORDER BY m.release_date DESC")
    List<Movie> selectMoviesByPersonId(@Param("personId") Long personId);

    /**
     * 原子递增电影播放量（避免并发丢失更新）
     *
     * @param movieId 电影ID
     * @return 影响行数
     */
    @Update("UPDATE movie SET views = views + 1 WHERE id = #{movieId}")
    int incrementViews(@Param("movieId") Long movieId);
}