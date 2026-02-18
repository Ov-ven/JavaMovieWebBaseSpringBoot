package com.software.movie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.software.movie.entity.MoviePerson;
import com.software.movie.mapper.MoviePersonMapper;
import com.software.movie.service.MoviePersonService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MoviePersonServiceImpl extends ServiceImpl<MoviePersonMapper, MoviePerson> implements MoviePersonService {
    @Override
    public List<MoviePerson> getMoviePersonsByMovieId(Long movieId) {
        QueryWrapper<MoviePerson> wrapper = new QueryWrapper<>();
        wrapper.eq("movie_id", movieId);
        return baseMapper.selectList(wrapper);
    }
}
