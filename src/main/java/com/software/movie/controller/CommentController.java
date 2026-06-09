package com.software.movie.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.software.movie.common.Result;
import com.software.movie.entity.Comment;
import com.software.movie.entity.User;
import com.software.movie.service.CommentService;
import com.software.movie.service.MovieService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;


/**
 * 评论管理控制器
 * <p>
 * 提供电影评论的 CRUD 接口，包括评论列表查询、添加评论、删除评论、更新评论等功能。
 * 所有写操作均需要用户登录，且修改/删除操作仅限评论所有者。
 * </p>
 */
@RestController
@RequestMapping("/api/comment")
public class CommentController {
    @Autowired
    private CommentService commentService;
    @Autowired
    private MovieService movieService;

    /**
     * 获取指定电影的评论列表（分页）
     *
     * @param pageNum  页码，默认 1
     * @param pageSize 每页条数，默认 10
     * @param movieId  电影 ID
     * @return 包含分页评论数据的统一响应结果
     */
    @GetMapping("/list")
    public Result getCommentList(@RequestParam(defaultValue = "1") Integer pageNum,
                                 @RequestParam(defaultValue = "10") Integer pageSize,
                                 @RequestParam Long movieId) {
        IPage<Comment> page = commentService.getCommentPageByMovieId(pageNum, pageSize, movieId);
        return Result.success(page);
    }

    /**
     * 添加评论
     * <p>
     * 用户登录后可以对电影发表评论。评论成功后会自动更新电影的评分。
     * </p>
     *
     * @param comment 评论实体，包含电影 ID、评论内容和评分
     * @param session HTTP 会话，用于获取当前登录用户信息
     * @return 操作结果，成功返回提示信息，未登录返回错误提示
     */
    @PostMapping("/add")
    public Result addComment(@RequestBody Comment comment, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error("请先登录");
        }

        comment.setUserId(user.getId());
        if (commentService.addComment(comment)) {
            // 更新电影评分
            movieService.updateMovieScore(comment.getMovieId());
            return Result.success("评论成功");
        }
        return Result.error("评论失败");
    }

    /**
     * 获取当前用户对指定电影的评论
     *
     * @param movieId 电影 ID
     * @param session HTTP 会话，用于获取当前登录用户信息
     * @return 包含评论数据的统一响应结果，未登录返回错误提示
     */
    @GetMapping("/my")
    public Result getMyComment(@RequestParam Long movieId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error("请先登录");
        }

        Comment comment = commentService.getCommentByUserAndMovie(user.getId(), movieId);
        return Result.success(comment);
    }

    /**
     * 删除评论
     * <p>
     * 仅允许评论所有者删除自己的评论。删除成功后会自动更新对应电影的评分。
     * </p>
     *
     * @param id      评论 ID
     * @param session HTTP 会话，用于获取当前登录用户信息
     * @return 操作结果，成功返回提示信息，无权或未登录返回错误提示
     */
    @DeleteMapping("/delete/{id}")
    public Result deleteComment(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error("请先登录");
        }

        // 检查是否是当前用户的评论
        if (!commentService.isUserCommentOwner(id, user.getId())) {
            return Result.error("无权删除此评论");
        }

        // 获取电影ID用于更新评分
        Comment comment = commentService.getById(id);
        Long movieId = comment != null ? comment.getMovieId() : null;

        if (commentService.removeComment(id)) {
            // 更新电影评分
            if (movieId != null) {
                movieService.updateMovieScore(movieId);
            }
            return Result.success("删除成功");
        }
        return Result.error("删除失败");
    }

    /**
     * 更新评论
     * <p>
     * 仅允许评论所有者修改自己的评论。更新成功后会自动更新对应电影的评分。
     * </p>
     *
     * @param comment 评论实体，包含评论 ID、新内容和评分
     * @param session HTTP 会话，用于获取当前登录用户信息
     * @return 操作结果，成功返回提示信息，无权或未登录返回错误提示
     */
    @PutMapping("/update")
    public Result updateComment(@RequestBody Comment comment, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error("请先登录");
        }

        // 检查是否是当前用户的评论
        if (!commentService.isUserCommentOwner(comment.getId(), user.getId())) {
            return Result.error("无权修改此评论");
        }

        if (commentService.updateComment(comment)) {
            // 更新电影评分
            movieService.updateMovieScore(comment.getMovieId());
            return Result.success("更新成功");
        }
        return Result.error("更新失败");
    }
}