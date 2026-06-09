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

/**
 * 评论服务实现类。
 * 提供评论的增删改查、平均评分计算及用户权限校验等功能。
 * 同一用户对同一电影仅保留一条评论，重复提交时自动更新。
 */
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * 获取电影评论分页列表
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param movieId  电影ID
     * @return 评论分页结果
     */
    @Override
    public IPage<Comment> getCommentPageByMovieId(Integer pageNum, Integer pageSize, Long movieId) {
        Page<Comment> page = new Page<>(pageNum, pageSize);
        return commentMapper.selectPageByMovieId(page, movieId);
    }

    /**
     * 添加或更新评论。
     * 若用户已对该电影评论过，则更新原有评论内容和评分。
     *
     * @param comment 评论实体
     * @return 操作是否成功
     */
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

    /**
     * 计算电影的平均评分
     *
     * @param movieId 电影ID
     * @return 平均评分，无评论时返回 null
     */
    @Override
    public Double calculateAverageScore(Long movieId) {
        return commentMapper.calculateAverageScore(movieId);
    }

    /**
     * 获取用户对某部电影的评论
     *
     * @param userId  用户ID
     * @param movieId 电影ID
     * @return 评论实体，不存在时返回 null
     */
    @Override
    public Comment getCommentByUserAndMovie(Long userId, Long movieId) {
        return commentMapper.selectByUserAndMovie(userId, movieId);
    }

    /**
     * 删除评论
     *
     * @param id 评论ID
     * @return 操作是否成功
     */
    @Override
    public boolean removeComment(Long id) {
        return this.removeById(id);
    }

    /**
     * 更新评论
     *
     * @param comment 评论实体
     * @return 操作是否成功
     */
    @Override
    public boolean updateComment(Comment comment) {
        return this.updateById(comment);
    }

    /**
     * 检查用户是否为评论的所有者
     *
     * @param commentId 评论ID
     * @param userId    用户ID
     * @return 是否为评论所有者
     */
    @Override
    public boolean isUserCommentOwner(Long commentId, Long userId) {
        QueryWrapper<Comment> wrapper = new QueryWrapper<>();
        wrapper.eq("id", commentId).eq("user_id", userId);
        return this.count(wrapper) > 0;
    }

    /**
     * 获取电影的所有评论列表
     *
     * @param movieId 电影ID
     * @return 评论列表
     */
    @Override
    public List<Comment> getCommentsByMovieId(Long movieId) {
        return commentMapper.selectCommentsByMovieId(movieId);
    }

    /**
     * 获取评论关联的用户信息
     *
     * @param userId 用户ID
     * @return 用户实体
     */
    @Override
    public User getUserForComment(Long userId) {
        return userMapper.selectById(userId);
    }
}
