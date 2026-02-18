package com.software.movie.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.software.movie.entity.Movie;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

public interface MovieService extends IService<Movie> {
    IPage<Movie> getMoviePage(Integer pageNum, Integer pageSize,
                              String type, String region,
                              String keyword, String sort);
    List<Movie> getHotMovies(Integer limit);

    List<Movie> getTopScoreMovies(Integer limit);

    Movie getMovieDetail(Long id);

    void increaseViews(Long movieId);

    List<Movie> getMoviesByPerson(Long personId);

    void updateMovieScore(Long movieId);

    List<Movie> getWeeklyTopMovies(Integer limit);

    List<Movie> getMonthlyTopMovies(Integer limit);
    List<Movie> getAllTimeTopMovies(Integer limit);
    List<Movie> getNewMovies(Integer limit);
    List<Movie> getAllMovies();


}