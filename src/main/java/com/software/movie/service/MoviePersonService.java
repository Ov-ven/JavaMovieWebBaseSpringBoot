package com.software.movie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.software.movie.entity.MoviePerson;

import java.util.List;

public interface MoviePersonService extends IService<MoviePerson> {
    List<MoviePerson> getMoviePersonsByMovieId(Long movieId);
}