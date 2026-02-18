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


@RestController
@RequestMapping("/api/comment")
public class CommentController {
    @Autowired
    private CommentService commentService;
    @Autowired
    private MovieService movieService;

    @GetMapping("/list")
    public Result getCommentList(@RequestParam(defaultValue = "1") Integer pageNum,
                                 @RequestParam(defaultValue = "10") Integer pageSize,
                                 @RequestParam Long movieId) {
        IPage<Comment> page = commentService.getCommentPageByMovieId(pageNum, pageSize, movieId);
        return Result.success(page);
    }

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

    @GetMapping("/my")
    public Result getMyComment(@RequestParam Long movieId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error("请先登录");
        }

        Comment comment = commentService.getCommentByUserAndMovie(user.getId(), movieId);
        return Result.success(comment);
    }

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