package com.software.movie.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.software.movie.entity.Comment;
import com.software.movie.entity.Movie;
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
@EnableCaching
@Controller
public class MovieDetailController {

    @Autowired
    private MovieService movieService;
    @Autowired
    private CommentService commentService;

    @Autowired
    private MoviePersonService moviePersonService; // 用于获取电影与主创的关联
    @Autowired
    private PersonService personService; // 用于获取主创人员的详细信息
    @Autowired
    private UserService userService; // 用于获取用户信息

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
        boolean canWatch = true; // 默认可以观看
        System.out.println("现在的canWatch值："+canWatch);
        System.out.println("现在的userisvip值："+user.getIsvip());
        System.out.println("现在的movieisvip值："+movie.getIsVip());
        if (movie.getIsVip()==1) { // 如果是VIP影片
            if (user != null && user.getIsvip()==0) { // 如果用户未登录或是VIP
                canWatch = false;
                System.out.println("现在的canWatch值："+canWatch);
            }
        }
        model.addAttribute("canWatch", canWatch);

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

                System.out.println("DEBUG - commentMap for commentId " + comment.getId() + ": " + commentMap);
            }
        }
        model.addAttribute("comments", commentsWithUserInfo);

        return "detail"; // 返回 Thymeleaf 模板名称
    }
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