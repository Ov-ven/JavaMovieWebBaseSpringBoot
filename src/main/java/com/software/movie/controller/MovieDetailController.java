package com.software.movie.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.software.movie.entity.Comment;
import com.software.movie.entity.Movie;
import com.software.movie.entity.Order;
import com.software.movie.entity.MoviePerson;
import com.software.movie.entity.Person;
import com.software.movie.entity.User;
import com.software.movie.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.stream.Collectors;
/**
 * 电影详情页面控制器
 * <p>
 * 负责展示电影详情页面，包括电影基本信息、主创人员、评论列表，
 * 以及通过表单提交添加评论的功能。同时处理播放权限校验。
 * </p>
 */
@EnableCaching
@Controller
public class MovieDetailController {

    @Autowired
    private MovieService movieService;
    @Autowired
    private CommentService commentService;
    @Autowired
    private com.software.movie.mapper.UserMovieEntitlementMapper entitlementMapper;

    @Autowired
    private MoviePersonService moviePersonService;
    @Autowired
    private PersonService personService;
    @Autowired
    private UserService userService;
    @Autowired
    private OrderService orderService;

    /**
     * 显示电影详情页
     * @param id 电影ID
     * @param pageNum 评论当前页码
     * @param pageSize 评论每页大小
     * @param model Spring MVC Model
     * @param session HttpSession 用于获取当前登录用户
     * @return 电影详情页视图名称
     */
    @GetMapping("/detail/{id}")
    public String getMovieDetail(@PathVariable Long id,
                                 @RequestParam(defaultValue = "1") Integer pageNum,
                                 @RequestParam(defaultValue = "10") Integer pageSize,
                                 Model model,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {

        model.addAttribute("queryType", null);
        model.addAttribute("queryRegion", null);
        model.addAttribute("queryKeyword", null);
        model.addAttribute("querySort", null);
        model.addAttribute("currentPage", 1);

        IPage<Movie> dummyMoviePage = new Page<>(1, 12);
        dummyMoviePage.setRecords(Collections.emptyList());
        dummyMoviePage.setTotal(0);
        model.addAttribute("moviePage", dummyMoviePage);

        // 1. 获取电影详情
        Movie movie = movieService.getMovieDetail(id);
        if (movie == null) {
            // 如果电影不存在，可以跳转到错误页面或者首页
            return "redirect:/"; // 或者 "error/404"
        }
        model.addAttribute("movie", movie);

        // 2. 增加电影播放量 (这里可以根据实际需求调整，比如在播放时才增加)
        movieService.increaseViews(id);


        User user = (User) session.getAttribute("user");
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "您尚未登录，请登录后查看电影详情。");
            return "redirect:/"; // 重定向到首页
        }
        // 3. 判断用户是否有观看权限
        boolean canWatch = true;
        boolean isVipMovie = Integer.valueOf(1).equals(movie.getIsVip());
        boolean isUserVip = Integer.valueOf(1).equals(user.getIsvip());
        boolean hasPrice = movie.getPrice() != null && movie.getPrice() > 0;

        // 优先查凭证表（最可靠），兜底查订单表
        if (isVipMovie && !isUserVip) {
            canWatch = hasEntitlement(user.getId(), id);
        } else if (!isVipMovie && hasPrice) {
            canWatch = hasEntitlement(user.getId(), id);
        }

        // 4. 检查是否有待支付订单（未过期）
        String pendingOrderNo = null;
        if (!canWatch && hasPrice) {
            Order pendingOrder = orderService.getPendingOrder(user.getId(), id);
            if (pendingOrder != null) {
                pendingOrderNo = pendingOrder.getOrderNo();
            }
        }

        model.addAttribute("canWatch", canWatch);
        model.addAttribute("pendingOrderNo", pendingOrderNo);

        // 4. 获取主创人员信息
        List<MoviePerson> moviePersons = moviePersonService.getMoviePersonsByMovieId(id); // 假设你在MoviePersonService中有一个方法来获取这些关联
        List<Person> creators = new ArrayList<>();
        if (moviePersons != null && !moviePersons.isEmpty()) {
            List<Long> personIds = moviePersons.stream()
                    .map(MoviePerson::getPersonId)
                    .collect(Collectors.toList());
            creators = personService.getPersonsByIds(personIds); // 假设你在PersonService中有一个方法来批量获取Person
        }
        model.addAttribute("creators", creators);
// 评论控制器中的代码片段
        List<Comment> comments = commentService.getCommentsByMovieId(id);
        List<Map<String, Object>> commentsWithUserInfo = new ArrayList<>();

        if (comments != null && !comments.isEmpty()) {
            for (Comment comment : comments) {
                Map<String, Object> commentMap = new LinkedHashMap<>(); // 保持顺序
                commentMap.put("commentId", comment.getId());
                commentMap.put("content", comment.getContent());
                commentMap.put("score", comment.getScore()); // 确保这里获取到的值是正确的，即使是null也行
                commentMap.put("createTime", comment.getCreateTime());

                User commentUser = commentService.getUserForComment(comment.getUserId());
                if (commentUser != null) {
                    commentMap.put("userName", commentUser.getUsername());
                } else {
                    commentMap.put("userName", "匿名用户");
                }
                commentsWithUserInfo.add(commentMap);

            }
        }
        model.addAttribute("comments", commentsWithUserInfo);

        return "detail"; // 返回 Thymeleaf 模板名称
    }
    /**
     * 检查用户是否拥有某部电影的观看凭证
     */
    private boolean hasEntitlement(Long userId, Long movieId) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.software.movie.entity.UserMovieEntitlement> ew =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        ew.eq("user_id", userId).eq("movie_id", movieId);
        return entitlementMapper.selectCount(ew) > 0;
    }

    /**
     * 通过表单提交添加评论
     * <p>
     * 用户登录后可以通过表单对电影发表评论和评分。操作完成后重定向回电影详情页。
     * </p>
     *
     * @param movieId            电影 ID
     * @param score              评分
     * @param content            评论内容
     * @param session            HTTP 会话，用于获取当前登录用户
     * @param redirectAttributes 重定向属性，用于传递 Flash 消息
     * @return 重定向到电影详情页，未登录则重定向到登录页
     */
    @PostMapping("/comment/add")
    public String addComment(@RequestParam("movieId") Long movieId,
                             @RequestParam("score") Double score,
                             @RequestParam("content") String content,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {

        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "请先登录才能发表评论！");
            return "redirect:/login";
        }

        Comment comment = new Comment();
        comment.setMovieId(movieId);
        comment.setUserId(currentUser.getId());
        comment.setScore(score);
        comment.setContent(content);

        try {
            boolean success = commentService.addComment(comment);
            if (success) {
                redirectAttributes.addFlashAttribute("success", "评论发表成功！");
            } else {
                redirectAttributes.addFlashAttribute("error", "评论发表失败，请稍后再试。");
            }
        } catch (Exception e) {
            // 记录异常日志
            System.err.println("Error adding comment: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "评论发表时发生错误：" + e.getMessage());
        }

        return "redirect:/detail/" + movieId;
    }
}