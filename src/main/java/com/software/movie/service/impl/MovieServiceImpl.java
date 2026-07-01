package com.software.movie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.software.movie.common.event.MovieDataChangeEvent;
import com.software.movie.entity.Movie;
import com.software.movie.entity.dto.MovieQueryDTO;
import com.software.movie.mapper.MovieMapper;
import com.software.movie.mapper.MoviePersonMapper;
import com.software.movie.service.CommentService;
import com.software.movie.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * 电影服务实现类。
 * 提供电影的分页查询、详情缓存、播放量统计、评分更新及各类榜单功能。
 * 使用 Spring Cache + Redis 实现多级缓存策略。
 */
@Service
public class MovieServiceImpl extends ServiceImpl<MovieMapper, Movie> implements MovieService {
    @Autowired
    private MovieMapper movieMapper;

    @Autowired
    private MoviePersonMapper moviePersonMapper;

    @Autowired
    private CommentService commentService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 分页查询电影列表，支持按类型、地区、关键词、VIP/免费筛选及排序。
     *
     * @param queryDTO 查询参数封装
     * @return 电影分页结果
     */
    @Override
    public IPage<Movie> getMoviePage(MovieQueryDTO queryDTO) {
        Integer pageNum = queryDTO.getPageNum() != null ? queryDTO.getPageNum() : 1;
        Integer pageSize = queryDTO.getPageSize() != null ? queryDTO.getPageSize() : 12;

        Page<Movie> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Movie> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);

        String type = queryDTO.getType();
        String region = queryDTO.getRegion();
        String keyword = queryDTO.getKeyword();
        String sort = queryDTO.getSort();
        Integer isVip = queryDTO.getIsVip();
        Integer free = queryDTO.getFree();

        if (StringUtils.isNotBlank(type)) wrapper.eq("type", type);
        if (StringUtils.isNotBlank(region)) wrapper.eq("region", region);
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like("title", keyword).or().like("description", keyword));
        }

        // VIP/免费筛选
        if (isVip != null && isVip == 1) {
            wrapper.eq("is_vip", 1);
        } else if (free != null && free == 1) {
            wrapper.and(w -> w.eq("is_vip", 0).and(w2 -> w2.isNull("price").or().le("price", 0)));
        }

        // 排序
        if ("hot".equals(sort)) wrapper.orderByDesc("views");
        else if ("top".equals(sort)) wrapper.orderByDesc("score");
        else if ("new".equals(sort)) wrapper.orderByDesc("release_date");
        else wrapper.orderByDesc("create_time");

        return baseMapper.selectPage(page, wrapper);
    }

    /**
     * 获取热门电影列表（按播放量排序）
     *
     * @param limit 返回数量
     * @return 热门电影列表
     */
    @Cacheable(cacheNames = "movies", key = "'hot_' + #limit", sync = true)
    @Override
    public List<Movie> getHotMovies(Integer limit) {
        return movieMapper.selectHotMovies(limit);
    }

    /**
     * 获取高分电影列表（按评分排序）
     *
     * @param limit 返回数量
     * @return 高分电影列表
     */
    @Cacheable(cacheNames = "movies", key = "'topScore_' + #limit", sync = true)
    @Override
    public List<Movie> getTopScoreMovies(Integer limit) {
        return movieMapper.selectTopScoreMovies(limit);
    }

    /**
     * 获取电影详情（含 Spring Cache 缓存）
     *
     * @param id 电影ID
     * @return 电影实体，不存在时返回 null
     */
    @Override
    @Cacheable(cacheNames = "movieDetail", key = "#id", sync = true)
    public Movie getMovieDetail(Long id) {
        return movieMapper.selectById(id);
    }

    /**
     * 增加电影播放量
     *
     * @param movieId 电影ID
     */
    @CacheEvict(cacheNames = {"movieDetail", "movies"}, allEntries = true, beforeInvocation = false)
    @Override
    public void increaseViews(Long movieId) {
        movieMapper.incrementViews(movieId);
    }

    /**
     * 获取指定主创参演/执导的电影列表
     *
     * @param personId 主创ID
     * @return 电影列表
     */
    @Cacheable(cacheNames = "movies", key = "'byPerson_' + #personId", sync = true)
    @Override
    public List<Movie> getMoviesByPerson(Long personId) {
        return movieMapper.selectMoviesByPersonId(personId);
    }

    /**
     * 重新计算并更新电影的平均评分
     *
     * @param movieId 电影ID
     */
    @CacheEvict(cacheNames = {"movieDetail", "movies"}, key = "#movieId", allEntries = false)
    @Override
    public void updateMovieScore(Long movieId) {
        Double avgScore = commentService.calculateAverageScore(movieId);
        if (avgScore != null) {
            Movie movie = this.getById(movieId);
            if (movie != null) {
                movie.setScore(avgScore);
                this.updateById(movie);
            }
        }
    }

    /**
     * 获取本周热播电影榜单（简化实现：按播放量排序）
     *
     * @param limit 返回数量
     * @return 本周热播电影列表
     */
    @Cacheable(cacheNames = "movies", key = "'weeklyTop_' + #limit", sync = true)
    @Override
    public List<Movie> getWeeklyTopMovies(Integer limit) {
        // 实际项目中应该基于播放记录计算本周热播
        // 这里简化实现：返回播放量最高的电影
        QueryWrapper<Movie> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("views")
                .last("LIMIT " + limit);
        return baseMapper.selectList(wrapper);
    }

    /**
     * 获取最新上架的电影列表
     *
     * @param limit 返回数量
     * @return 最新电影列表
     */
    @Cacheable(cacheNames = "movies", key = "'newMovies_' + #limit", sync = true)
    @Override
    public List<Movie> getNewMovies(Integer limit) {
        // 获取最新上架的电影
        QueryWrapper<Movie> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("release_date")
                .last("LIMIT " + limit);
        return baseMapper.selectList(wrapper);
    }

    /**
     * 获取本月最佳电影榜单（按本月发布电影的评分排序）
     *
     * @param limit 返回数量
     * @return 本月最佳电影列表
     */
    @Cacheable(cacheNames = "movies", key = "'monthlyTop_' + #limit", sync = true)
    @Override
    public List<Movie> getMonthlyTopMovies(Integer limit) {
        QueryWrapper<Movie> wrapper = new QueryWrapper<>();
        // 假设release_date字段表示电影发布日期，筛选本月的电影
        wrapper.ge("release_date", LocalDate.now().withDayOfMonth(1))
                .le("release_date", LocalDate.now())
                .orderByDesc("score")
                .last("LIMIT " + limit);
        return baseMapper.selectList(wrapper);
    }

    /**
     * 获取历史最佳电影榜单（按评分排序）
     *
     * @param limit 返回数量
     * @return 历史最佳电影列表
     */
    @Cacheable(cacheNames = "movies", key = "'allTimeTop_' + #limit", sync = true)
    @Override
    public List<Movie> getAllTimeTopMovies(Integer limit) {
        QueryWrapper<Movie> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("score")
                .last("LIMIT " + limit);
        return baseMapper.selectList(wrapper);
    }

    /**
     * 获取所有电影列表
     *
     * @return 全部电影列表
     */
    @Override
    public List<Movie> getAllMovies() {
        return list();
    }

    // ========== 向量库增量同步：重写 CRUD 方法，发布领域事件 ==========

    @Override
    @Transactional
    public boolean save(Movie movie) {
        boolean result = super.save(movie);
        if (result && movie.getId() != null) {
            eventPublisher.publishEvent(new MovieDataChangeEvent(movie.getId(), MovieDataChangeEvent.UPSERT));
        }
        return result;
    }

    @Override
    @Transactional
    public boolean updateById(Movie movie) {
        boolean result = super.updateById(movie);
        if (result && movie.getId() != null) {
            eventPublisher.publishEvent(new MovieDataChangeEvent(movie.getId(), MovieDataChangeEvent.UPSERT));
        }
        return result;
    }

    @Override
    @Transactional
    public boolean removeById(Serializable id) {
        boolean result = super.removeById(id);
        if (result) {
            eventPublisher.publishEvent(new MovieDataChangeEvent((Long) id, MovieDataChangeEvent.DELETE));
        }
        return result;
    }

    @Override
    @Transactional
    public boolean removeByIds(Collection<?> idList) {
        boolean result = super.removeByIds(idList);
        if (result) {
            for (Object id : idList) {
                eventPublisher.publishEvent(new MovieDataChangeEvent((Long) id, MovieDataChangeEvent.DELETE));
            }
        }
        return result;
    }
}
