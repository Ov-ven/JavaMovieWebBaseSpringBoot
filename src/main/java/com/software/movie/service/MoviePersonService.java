package com.software.movie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.software.movie.entity.MoviePerson;

import java.util.List;

/**
 * 电影-主创关联服务接口。
 * 提供根据电影ID查询关联主创（演员/导演）信息的功能。
 */
public interface MoviePersonService extends IService<MoviePerson> {

    /**
     * 获取电影的所有主创关联记录
     *
     * @param movieId 电影ID
     * @return 电影-主创关联列表
     */
    List<MoviePerson> getMoviePersonsByMovieId(Long movieId);
}
