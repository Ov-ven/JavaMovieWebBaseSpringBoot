package com.software.movie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.software.movie.entity.Comment;
import com.software.movie.entity.User;
import com.software.movie.mapper.CommentMapper;
import com.software.movie.mapper.UserMapper;
import com.software.movie.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private UserMapper userMapper;
    @Override
    public IPage<Comment> getCommentPageByMovieId(Integer pageNum, Integer pageSize, Long movieId) {
        Page<Comment> page = new Page<>(pageNum, pageSize);
        return commentMapper.selectPageByMovieId(page, movieId);
    }

    @Override
    public boolean addComment(Comment comment) {
        // 检查用户是否已经评论过该电影
        Comment existing = commentMapper.selectByUserAndMovie(comment.getUserId(), comment.getMovieId());
        if (existing != null) {
            // 如果已评论，则更新原有评论
            existing.setContent(comment.getContent());
            existing.setScore(comment.getScore());
            return this.updateById(existing);
        }
        return this.save(comment);
    }

    @Override
    public Double calculateAverageScore(Long movieId) {
        return commentMapper.calculateAverageScore(movieId);
    }

    @Override
    public Comment getCommentByUserAndMovie(Long userId, Long movieId) {
        return commentMapper.selectByUserAndMovie(userId, movieId);
    }

    @Override
    public boolean removeComment(Long id) {
        return this.removeById(id);
    }

    @Override
    public boolean updateComment(Comment comment) {
        return this.updateById(comment);
    }

    @Override
    public boolean isUserCommentOwner(Long commentId, Long userId) {
        QueryWrapper<Comment> wrapper = new QueryWrapper<>();
        wrapper.eq("id", commentId).eq("user_id", userId);
        return this.count(wrapper) > 0;
    }

    @Override
    public List<Comment> getCommentsByMovieId(Long movieId) {
        return commentMapper.selectCommentsByMovieId(movieId);
    }


    @Override
    public User getUserForComment(Long userId) {
        return userMapper.selectById(userId);
    }
}