package com.software.movie.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.software.movie.entity.Movie;
import com.software.movie.entity.Order;
import com.software.movie.entity.User;
import com.software.movie.entity.dto.MovieQueryDTO;
import com.software.movie.mapper.MovieMapper;
import com.software.movie.service.MovieService;
import com.software.movie.service.OrderService;
import com.software.movie.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.List;

/**
 * 首页及公共页面控制器
 * <p>
 * 负责首页电影列表展示、用户注册/登录页面跳转、登出处理、
 * 个人中心页面、VIP 升级页面等公共页面的路由。
 * </p>
 */
@Controller
public class HomeController {

    @Autowired
    private MovieService movieService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private MovieMapper movieMapper;

    /**
     * 首页入口
     * <p>
     * 展示电影列表（支持按类型、地区、关键词筛选和排序），同时提供轮播图热播电影、
     * 高分排行榜、本周热播、新片推荐等数据。
     * </p>
     *
     * @param queryDTO 查询参数封装（pageNum, pageSize, type, region, keyword, sort, isVip, free）
     * @param model    Spring MVC 模型对象
     * @return index 视图名称
     */
    @GetMapping("/")
    public String index(MovieQueryDTO queryDTO, Model model) {

        // 确保页码有效
        if (queryDTO.getPageNum() == null || queryDTO.getPageNum() < 1) queryDTO.setPageNum(1);

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
        IPage<Movie> moviePage = movieService.getMoviePage(queryDTO);

        // 处理moviePage为null的情况
        if (moviePage == null) {
            moviePage = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
            moviePage.setRecords(Collections.emptyList());
            moviePage.setTotal(0);
        }

        model.addAttribute("moviePage", moviePage);

        // 计算上一页和下一页
        int prevPage = Math.max(1, queryDTO.getPageNum() - 1);
        int nextPage = Math.min((int) moviePage.getPages(), queryDTO.getPageNum() + 1);

        // 添加模型属性
        model.addAttribute("currentPage", queryDTO.getPageNum());
        model.addAttribute("prevPage", prevPage);
        model.addAttribute("nextPage", nextPage);

        // 保留查询参数
        model.addAttribute("queryType", queryDTO.getType());
        model.addAttribute("queryRegion", queryDTO.getRegion());
        model.addAttribute("queryKeyword", queryDTO.getKeyword());
        model.addAttribute("querySort", queryDTO.getSort());

        return "index";
    }

    /**
     * 显示用户注册页面
     *
     * @return register 视图名称
     */
    @GetMapping("/register")
    public String showRegisterPage() {
        return "register";
    }

    /**
     * 显示用户登录页面
     *
     * @return login 视图名称
     */
    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    /**
     * 用户登出
     * <p>
     * 销毁当前会话并重定向到登录页面。
     * </p>
     *
     * @param session HTTP 会话
     * @return 重定向到登录页面
     */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    /**
     * 显示用户个人中心页面
     * <p>
     * 从会话中获取当前登录用户信息并展示个人资料页。
     * </p>
     *
     * @param session HTTP 会话，用于获取当前登录用户
     * @return profile 视图名称
     */
    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        // 刷新用户信息（确保 balance 等新字段已加载）
        User freshUser = userService.getById(user.getId());
        if (freshUser != null) {
            session.setAttribute("user", freshUser);
        }
        // header 模板需要的占位变量
        model.addAttribute("queryType", null);
        model.addAttribute("queryRegion", null);
        model.addAttribute("queryKeyword", null);
        model.addAttribute("querySort", null);
        model.addAttribute("currentPage", 1);
        IPage<Movie> dummyPage = new Page<>(1, 12);
        dummyPage.setRecords(Collections.emptyList());
        dummyPage.setTotal(0);
        model.addAttribute("moviePage", dummyPage);
        return "profile";
    }

    /**
     * 显示 VIP 升级页面
     * <p>
     * 用户未登录时重定向到登录页面；已登录时将用户信息传递给前端。
     * </p>
     *
     * @param session HTTP 会话，用于获取当前登录用户
     * @param model   Spring MVC 模型对象
     * @return upgrade 视图名称，或重定向到登录页面
     */
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

    /**
     * 收银台页面
     */
    @GetMapping("/pay")
    public String payPage(@RequestParam String orderNo, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        Order order = orderService.getByOrderNo(orderNo);
        if (order == null || !order.getUserId().equals(user.getId())) {
            return "redirect:/profile";
        }
        if (order.getStatus() != 0) {
            return "redirect:/profile";
        }

        // 刷新用户余额
        User freshUser = userService.getById(user.getId());
        String movieTitle = "-";
        if (order.getMovieId() != null) {
            Movie movie = movieMapper.selectById(order.getMovieId());
            if (movie != null) movieTitle = movie.getTitle();
        }

        // 计算剩余需支付金额（扣除已用余额支付的部分）
        double balancePaid = order.getBalancePaid() == null ? 0 : order.getBalancePaid();
        double remainingAmount = order.getAmount() - balancePaid;

        model.addAttribute("orderNo", order.getOrderNo());
        model.addAttribute("amount", remainingAmount);  // 显示剩余金额，不是全额
        model.addAttribute("totalAmount", order.getAmount());  // 订单总额
        model.addAttribute("balancePaid", balancePaid);  // 已用余额支付
        model.addAttribute("balance", freshUser.getBalance() == null ? 0.0 : freshUser.getBalance());
        model.addAttribute("movieTitle", movieTitle);

        // header 占位变量
        model.addAttribute("queryType", null);
        model.addAttribute("queryRegion", null);
        model.addAttribute("queryKeyword", null);
        model.addAttribute("querySort", null);
        model.addAttribute("currentPage", 1);
        IPage<Movie> dummyPage = new Page<>(1, 12);
        dummyPage.setRecords(Collections.emptyList());
        dummyPage.setTotal(0);
        model.addAttribute("moviePage", dummyPage);

        return "pay";
    }

    /**
     * AI 智能助手聊天页面
     */
    @GetMapping("/chat")
    public String chatPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        // header 占位变量
        model.addAttribute("queryType", null);
        model.addAttribute("queryRegion", null);
        model.addAttribute("queryKeyword", null);
        model.addAttribute("querySort", null);
        model.addAttribute("currentPage", 1);
        IPage<Movie> dummyPage = new Page<>(1, 12);
        dummyPage.setRecords(Collections.emptyList());
        dummyPage.setTotal(0);
        model.addAttribute("moviePage", dummyPage);

        return "chat";
    }

    /**
     * 我的收藏页面
     */
    @GetMapping("/favorites")
    public String favoritesPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        // header 占位变量
        model.addAttribute("queryType", null);
        model.addAttribute("queryRegion", null);
        model.addAttribute("queryKeyword", null);
        model.addAttribute("querySort", null);
        model.addAttribute("currentPage", 1);
        IPage<Movie> dummyPage = new Page<>(1, 12);
        dummyPage.setRecords(Collections.emptyList());
        dummyPage.setTotal(0);
        model.addAttribute("moviePage", dummyPage);

        return "favorites";
    }

    /**
     * 显示 VIP 管理页面
     *
     * @return manage 视图名称
     */
    @GetMapping("/vip/manage")
    public String managePage() {
        return "manage"; // 对应模板文件 vip/manage.html
    }
}