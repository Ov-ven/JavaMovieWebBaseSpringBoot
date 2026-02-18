package com.software.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.software.movie.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    IPage<Comment> selectPageByMovieId(Page<Comment> page, @Param("movieId") Long movieId);


    Double calculateAverageScore(@Param("movieId") Long movieId);


    Comment selectByUserAndMovie(@Param("userId") Long userId, @Param("movieId") Long movieId);
    List<Comment> selectCommentsByMovieId(Long movieId);

    int updateComment(Comment comment);
}