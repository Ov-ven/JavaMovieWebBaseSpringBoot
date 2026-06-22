package com.software.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.software.movie.entity.Comment;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 电影评论数据访问层接口。
 * 继承 MyBatis-Plus 的 BaseMapper，提供评论的 CRUD 及自定义查询方法。
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    /**
     * 根据电影ID分页查询评论列表。
     *
     * @param page    分页参数
     * @param movieId 电影ID
     * @return 评论分页结果
     */
    IPage<Comment> selectPageByMovieId(Page<Comment> page, @Param("movieId") Long movieId);

    /**
     * 计算指定电影的平均评分。
     *
     * @param movieId 电影ID
     * @return 平均评分，无评论时返回null
     */
    Double calculateAverageScore(@Param("movieId") Long movieId);

    /**
     * 根据用户ID和电影ID查询该用户的评论。
     *
     * @param userId  用户ID
     * @param movieId 电影ID
     * @return 评论实体，不存在时返回null
     */
    Comment selectByUserAndMovie(@Param("userId") Long userId, @Param("movieId") Long movieId);

    /**
     * 根据电影ID查询所有评论（不分页）。
     *
     * @param movieId 电影ID
     * @return 评论列表
     */
    List<Comment> selectCommentsByMovieId(Long movieId);

    /**
     * 更新评论内容和评分。
     *
     * @param comment 评论实体（需包含id、content、score）
     * @return 受影响行数
     */
    int updateComment(Comment comment);

    /**
     * 插入或更新评论（基于 user_id + movie_id 唯一索引）。
     * <p>如果该用户已评论过该电影，则更新内容和评分；否则插入新评论。</p>
     *
     * @param userId  用户ID
     * @param movieId 电影ID
     * @param content 评论内容
     * @param score   评分
     * @return 受影响行数（1=新增，2=更新）
     */
    @Insert("INSERT INTO comment (user_id, movie_id, content, score) " +
            "VALUES (#{userId}, #{movieId}, #{content}, #{score}) " +
            "ON DUPLICATE KEY UPDATE content = VALUES(content), score = VALUES(score)")
    int upsertComment(@Param("userId") Long userId,
                      @Param("movieId") Long movieId,
                      @Param("content") String content,
                      @Param("score") Double score);
}