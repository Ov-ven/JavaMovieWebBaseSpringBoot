package com.software.movie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.software.movie.entity.MoviePerson;
import com.software.movie.mapper.MoviePersonMapper;
import com.software.movie.service.MoviePersonService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 电影-主创关联服务实现类。
 * 提供根据电影ID查询关联主创（演员/导演）信息的功能。
 */
@Service
public class MoviePersonServiceImpl extends ServiceImpl<MoviePersonMapper, MoviePerson> implements MoviePersonService {

    /**
     * 获取电影的所有主创关联记录
     *
     * @param movieId 电影ID
     * @return 电影-主创关联列表
     */
    @Override
    public List<MoviePerson> getMoviePersonsByMovieId(Long movieId) {
        QueryWrapper<MoviePerson> wrapper = new QueryWrapper<>();
        wrapper.eq("movie_id", movieId);
        return baseMapper.selectList(wrapper);
    }
}
