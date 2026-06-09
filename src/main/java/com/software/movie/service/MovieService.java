package com.software.movie.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.software.movie.entity.Movie;
import com.software.movie.entity.dto.MovieQueryDTO;

import java.util.List;

/**
 * 电影服务接口。
 * 提供电影的分页查询、详情查看、播放量统计、评分更新及各类榜单功能。
 */
public interface MovieService extends IService<Movie> {

    /**
     * 分页查询电影列表，支持按类型、地区、关键词、VIP/免费筛选及排序。
     * <p>入参使用 {@link MovieQueryDTO} 封装，便于 LLM Function Calling 调用。</p>
     *
     * @param queryDTO 查询参数封装
     * @return 电影分页结果
     */
    IPage<Movie> getMoviePage(MovieQueryDTO queryDTO);

    /**
     * 获取热门电影列表（按播放量排序）
     *
     * @param limit 返回数量
     * @return 热门电影列表
     */
    List<Movie> getHotMovies(Integer limit);

    /**
     * 获取高分电影列表（按评分排序）
     *
     * @param limit 返回数量
     * @return 高分电影列表
     */
    List<Movie> getTopScoreMovies(Integer limit);

    /**
     * 获取电影详情（含缓存）
     *
     * @param id 电影ID
     * @return 电影实体，不存在时返回 null
     */
    Movie getMovieDetail(Long id);

    /**
     * 增加电影播放量
     *
     * @param movieId 电影ID
     */
    void increaseViews(Long movieId);

    /**
     * 获取指定主创参演/执导的电影列表
     *
     * @param personId 主创ID
     * @return 电影列表
     */
    List<Movie> getMoviesByPerson(Long personId);

    /**
     * 重新计算并更新电影的平均评分
     *
     * @param movieId 电影ID
     */
    void updateMovieScore(Long movieId);

    /**
     * 获取本周热播电影榜单
     *
     * @param limit 返回数量
     * @return 本周热播电影列表
     */
    List<Movie> getWeeklyTopMovies(Integer limit);

    /**
     * 获取本月最佳电影榜单
     *
     * @param limit 返回数量
     * @return 本月最佳电影列表
     */
    List<Movie> getMonthlyTopMovies(Integer limit);

    /**
     * 获取历史最佳电影榜单
     *
     * @param limit 返回数量
     * @return 历史最佳电影列表
     */
    List<Movie> getAllTimeTopMovies(Integer limit);

    /**
     * 获取最新上架的电影列表
     *
     * @param limit 返回数量
     * @return 最新电影列表
     */
    List<Movie> getNewMovies(Integer limit);

    /**
     * 获取所有电影列表
     *
     * @return 全部电影列表
     */
    List<Movie> getAllMovies();
}
