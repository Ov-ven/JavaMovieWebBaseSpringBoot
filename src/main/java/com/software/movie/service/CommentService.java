package com.software.movie.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.software.movie.entity.Comment;
import com.software.movie.entity.User;

import java.util.List;

public interface CommentService extends IService<Comment> {
    // 获取电影评论分页
    IPage<Comment> getCommentPageByMovieId(Integer pageNum, Integer pageSize, Long movieId);

    // 添加或更新评论
    boolean addComment(Comment comment);

    // 计算电影平均评分
    Double calculateAverageScore(Long movieId);

    // 获取用户对某部电影的评论
    Comment getCommentByUserAndMovie(Long userId, Long movieId);

    // 删除评论（新增）
    boolean removeComment(Long id);

    // 更新评论（新增）
    boolean updateComment(Comment comment);

    // 检查用户是否有权操作评论（新增）
    boolean isUserCommentOwner(Long commentId, Long userId);

    User getUserForComment(Long userId);
    List<Comment> getCommentsByMovieId(Long movieId);

}