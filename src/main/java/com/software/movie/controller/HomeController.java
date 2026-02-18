package com.software.movie.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.software.movie.entity.Movie;
import com.software.movie.entity.User;
import com.software.movie.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

@Controller
public class HomeController {

    @Autowired
    private MovieService movieService;
    @GetMapping("/")
    public String index(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort,
            Model model) {

        // 确保页码有效
        if (page < 1) page = 1;

        // 获取热播电影（用于轮播图）
        List<Movie> hotMovies = movieService.getHotMovies(5);
        model.addAttribute("hotMovies", hotMovies);

        // 获取高分电影（用于排行榜）
        List<Movie> topScoreMovies = movieService.getTopScoreMovies(5);
        model.addAttribute("topScoreMovies", topScoreMovies);

        // 获取本周热播（用于排行榜）
        List<Movie> weeklyTop = movieService.getWeeklyTopMovies(5);
        model.addAttribute("weeklyTop", weeklyTop);

        // 获取新片推荐（用于排行榜）
        List<Movie> newMovies = movieService.getNewMovies(5);
        model.addAttribute("newMovies", newMovies);

        // 获取电影列表（分页）
        IPage<Movie> moviePage = movieService.getMoviePage(page, size, type, region, keyword, sort);

        // 处理moviePage为null的情况
        if (moviePage == null) {
            // 创建一个空的IPage实例
            moviePage = new Page<>(page, size);
            moviePage.setRecords(Collections.emptyList());
            moviePage.setTotal(0);
        }

        model.addAttribute("moviePage", moviePage);

        // 计算上一页和下一页
        int prevPage = Math.max(1, page - 1);
        int nextPage = Math.min((int) moviePage.getPages(), page + 1);

        // 添加模型属性
        model.addAttribute("currentPage", page);
        model.addAttribute("prevPage", prevPage);
        model.addAttribute("nextPage", nextPage);

        // 保留查询参数
        model.addAttribute("queryType", type);
        model.addAttribute("queryRegion", region);
        model.addAttribute("queryKeyword", keyword);
        model.addAttribute("querySort", sort);

        return "index";
    }

    @GetMapping("/register")
    public String showRegisterPage() {
        return "register";
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/profile")
    public String profile(HttpSession session) {
        User user = (User) session.getAttribute("user");
        System.out.println("User methods:");
        for (Method method : user.getClass().getMethods()) {
            System.out.println(method.getName());
        }
        return "profile";
    }

    @GetMapping("/vip/upgrade") // 处理 /vip/upgrade 请求
    public String showUpgradePage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            // 如果用户未登录，重定向到登录页面
            return "redirect:/login";
        }
        // 将用户信息传递给前端，upgrade.html可能需要显示用户的当前VIP状态等信息
        model.addAttribute("user", user);
        return "upgrade"; // 返回 upgrade.html 模板
    }

    @GetMapping("/vip/manage")
    public String managePage() {
        return "manage"; // 对应模板文件 vip/manage.html
    }
}