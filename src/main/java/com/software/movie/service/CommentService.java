package com.software.movie.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.software.movie.entity.Comment;
import com.software.movie.entity.User;

import java.util.List;

/**
 * 评论服务接口。
 * 提供电影评论的增删改查、平均评分计算及权限校验等功能。
 */
public interface CommentService extends IService<Comment> {

    /**
     * 获取电影评论分页列表
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param movieId  电影ID
     * @return 评论分页结果
     */
    IPage<Comment> getCommentPageByMovieId(Integer pageNum, Integer pageSize, Long movieId);

    /**
     * 添加或更新评论。
     * 若用户已对该电影评论过，则更新原有评论内容和评分。
     *
     * @param comment 评论实体
     * @return 操作是否成功
     */
    boolean addComment(Comment comment);

    /**
     * 计算电影的平均评分
     *
     * @param movieId 电影ID
     * @return 平均评分，无评论时返回 null
     */
    Double calculateAverageScore(Long movieId);

    /**
     * 获取用户对某部电影的评论
     *
     * @param userId  用户ID
     * @param movieId 电影ID
     * @return 评论实体，不存在时返回 null
     */
    Comment getCommentByUserAndMovie(Long userId, Long movieId);

    /**
     * 删除评论
     *
     * @param id 评论ID
     * @return 操作是否成功
     */
    boolean removeComment(Long id);

    /**
     * 更新评论
     *
     * @param comment 评论实体
     * @return 操作是否成功
     */
    boolean updateComment(Comment comment);

    /**
     * 检查用户是否为评论的所有者
     *
     * @param commentId 评论ID
     * @param userId    用户ID
     * @return 是否为评论所有者
     */
    boolean isUserCommentOwner(Long commentId, Long userId);

    /**
     * 获取评论关联的用户信息
     *
     * @param userId 用户ID
     * @return 用户实体
     */
    User getUserForComment(Long userId);

    /**
     * 获取电影的所有评论列表
     *
     * @param movieId 电影ID
     * @return 评论列表
     */
    List<Comment> getCommentsByMovieId(Long movieId);
}
