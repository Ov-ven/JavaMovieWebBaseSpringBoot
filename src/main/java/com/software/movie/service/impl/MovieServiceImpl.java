package com.software.movie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.software.movie.entity.Movie;
import com.software.movie.mapper.MovieMapper;
import com.software.movie.mapper.MoviePersonMapper;
import com.software.movie.service.CommentService;
import com.software.movie.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class MovieServiceImpl extends ServiceImpl<MovieMapper, Movie> implements MovieService {
    @Autowired
    private MovieMapper movieMapper;


    @Autowired
    private MoviePersonMapper moviePersonMapper;

    @Autowired
    private CommentService commentService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


    @Cacheable(cacheNames = "movies", key = "'list_' + #pageNum + '_' + #pageSize + '_' + #type + '_' + #region + '_' + #keyword + '_' + #sort")
    @Override
    public IPage<Movie> getMoviePage(Integer pageNum, Integer pageSize,
                                     String type, String region,
                                     String keyword, String sort) {
        Page<Movie> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Movie> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1); // 只显示上架电影

        // 添加类型条件
        if (StringUtils.isNotBlank(type)) {
            wrapper.eq("type", type);
        }

        // 添加地区条件
        if (StringUtils.isNotBlank(region)) {
            wrapper.eq("region", region);
        }

        // 添加关键词搜索
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.like("title", keyword).or().like("description", keyword);
        }

        // 添加排序
        if ("hot".equals(sort)) {
            wrapper.orderByDesc("views");
        } else if ("top".equals(sort)) {
            wrapper.orderByDesc("score");
        } else if ("new".equals(sort)) {
            wrapper.orderByDesc("release_date");
        } else {
            wrapper.orderByDesc("create_time");
        }

        return baseMapper.selectPage(page, wrapper);
    }
    @Cacheable(cacheNames = "movies", key = "'hot_' + #limit")
    @Override
    public List<Movie> getHotMovies(Integer limit) {
        return movieMapper.selectHotMovies(limit);
    }
    @Cacheable(cacheNames = "movies", key = "'topScore_' + #limit")
    @Override
    public List<Movie> getTopScoreMovies(Integer limit) {
        return movieMapper.selectTopScoreMovies(limit);
    }

    @Override
    public Movie getMovieDetail(Long id) {
        Object object=redisTemplate.opsForValue().get("movieDetail::"+id);

        if(object!=null){
            return (Movie) object;
        }else{
            Movie movie = movieMapper.selectById(id);
            redisTemplate.opsForValue().set("movieDetail::"+id,movie);
            return movie;
        }
    }
    @CacheEvict(cacheNames = {"movieDetail", "movies"}, allEntries = true, beforeInvocation = false)
    @Override
    public void increaseViews(Long movieId) {
        Movie movie = movieMapper.selectById(movieId);
        if (movie != null) {
            movie.setViews(movie.getViews() + 1);
            movieMapper.updateById(movie);
        }
    }
    @Cacheable(cacheNames = "movies", key = "'byPerson_' + #personId")
    @Override
    public List<Movie> getMoviesByPerson(Long personId) {
        return movieMapper.selectMoviesByPersonId(personId);
    }
    @CacheEvict(cacheNames = {"movieDetail", "movies"}, key = "#movieId", allEntries = false) // 清除特定电影详情
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
    @Cacheable(cacheNames = "movies", key = "'weeklyTop_' + #limit")
    @Override
    public List<Movie> getWeeklyTopMovies(Integer limit) {
        // 实际项目中应该基于播放记录计算本周热播
        // 这里简化实现：返回播放量最高的电影
        QueryWrapper<Movie> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("views")
                .last("LIMIT " + limit);
        return baseMapper.selectList(wrapper);
    }
    @Cacheable(cacheNames = "movies", key = "'newMovies_' + #limit")
    @Override
    public List<Movie> getNewMovies(Integer limit) {
        // 获取最新上架的电影
        QueryWrapper<Movie> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("release_date")
                .last("LIMIT " + limit);
        return baseMapper.selectList(wrapper);
    }
    @Cacheable(cacheNames = "movies", key = "'monthlyTop_' + #limit")
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
    @Cacheable(cacheNames = "movies", key = "'allTimeTop_' + #limit")
    @Override
    public List<Movie> getAllTimeTopMovies(Integer limit) {
        QueryWrapper<Movie> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("score")
                .last("LIMIT " + limit);
        return baseMapper.selectList(wrapper);
    }
    @Override
    public List<Movie> getAllMovies() {
        return list(); // 调用 MyBatis-Plus ServiceImpl 提供的 list() 方法查询所有电影
    }
}