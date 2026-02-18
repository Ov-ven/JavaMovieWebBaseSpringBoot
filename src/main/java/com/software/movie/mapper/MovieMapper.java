package com.software.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.software.movie.entity.Movie;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MovieMapper extends BaseMapper<Movie> {
    /**
     * 条件分页查询电影
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
     * 获取热门电影（按观看次数排序）
     */
    @Select("SELECT * FROM movie WHERE status = 1 ORDER BY views DESC LIMIT #{limit}")
    List<Movie> selectHotMovies(@Param("limit") Integer limit);

    /**
     * 获取高分电影（按评分排序）
     */
    @Select("SELECT * FROM movie WHERE status = 1 AND score > 0 ORDER BY score DESC LIMIT #{limit}")
    List<Movie> selectTopScoreMovies(@Param("limit") Integer limit);

    /**
     * 根据影人ID查询相关电影
     */
    @Select("SELECT m.* FROM movie m " +
            "JOIN movie_person mp ON m.id = mp.movie_id " +
            "WHERE mp.person_id = #{personId} AND m.status = 1 " +
            "ORDER BY m.release_date DESC")
    List<Movie> selectMoviesByPersonId(@Param("personId") Long personId);
}