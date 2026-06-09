package com.software.movie.controller;

import com.software.movie.common.Result;
import com.software.movie.entity.User;
import com.software.movie.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

/**
 * 用户管理控制器
 * <p>
 * 提供用户相关的 RESTful API，包括用户登录、注册、用户名校验、
 * 获取用户信息、更新用户资料、VIP 升级和登出等功能。
 * </p>
 */
@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private UserService userService;

    /**
     * 用户登录
     *
     * @param user    包含用户名和密码的用户实体
     * @param session HTTP 会话，登录成功后写入用户信息
     * @return 登录成功返回用户信息，失败返回错误提示
     */
    @PostMapping("/login")
    public Result login(@RequestBody User user, HttpSession session) {
        User loginUser = userService.login(user.getUsername(), user.getPassword());
        if (loginUser != null) {
            session.setAttribute("user", loginUser);
            return Result.success(loginUser);
        }
        return Result.error("用户名或密码错误");
    }

    /**
     * 用户注册
     *
     * @param user 包含注册信息的用户实体
     * @return 注册成功返回提示信息，用户名已存在返回错误提示
     */
    @PostMapping("/register")
    public Result register(@RequestBody User user) {
        if (userService.register(user)) {
            return Result.success("注册成功");
        }
        return Result.error("用户名已存在");
    }

    /**
     * 校验用户名是否可用
     *
     * @param username 待校验的用户名
     * @return 校验结果，true 表示可用，false 表示已存在
     */
    @GetMapping("/checkUsername")
    public Result checkUsername(@RequestParam String username) {
        return Result.success(userService.checkUsername(username));
    }

    /**
     * 获取当前登录用户信息
     *
     * @param session HTTP 会话，用于获取当前登录用户
     * @return 用户信息，未登录返回错误提示
     */
    @GetMapping("/info")
    public Result getInfo(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            return Result.success(user);
        }
        return Result.error("未登录");
    }

    /**
     * 更新当前登录用户的资料
     *
     * @param user    包含待更新信息的用户实体
     * @param session HTTP 会话，用于获取和刷新当前登录用户
     * @return 更新成功返回提示信息，未登录返回错误提示
     */
    @PostMapping("/update")
    public Result updateInfo(@RequestBody User user, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error("未登录");
        }

        user.setId(currentUser.getId());
        if (userService.updateUserInfo(user)) {
            session.setAttribute("user", userService.getById(currentUser.getId()));
            return Result.success("更新成功");
        }
        return Result.error("更新失败");
    }

    /**
     * 升级当前用户为 VIP
     * <p>
     * 将当前登录用户升级为 VIP 会员，并刷新会话中的用户信息。
     * </p>
     *
     * @param session HTTP 会话，用于获取和刷新当前登录用户
     * @return 升级成功返回提示信息，未登录返回错误提示
     */
    @PostMapping("/upgradeVip")
    public Result upgradeVip(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error("未登录");
        }

        if (userService.upgradeToVip(user.getId())) {
            session.setAttribute("user", userService.getByUsername(user.getUsername()));
            return Result.success("升级VIP成功");
        }
        return Result.error("升级VIP失败");
    }

    /**
     * 余额充值（直接成功，无需沙箱支付）
     *
     * @param amount  充值金额
     * @param session HTTP 会话
     * @return 充值结果
     */
    @PostMapping("/recharge")
    public Result recharge(@RequestParam Double amount, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return Result.error("未登录");
        if (amount == null || amount <= 0) return Result.error("充值金额必须大于0");

        if (userService.recharge(user.getId(), amount)) {
            session.setAttribute("user", userService.getById(user.getId()));
            return Result.success("充值成功");
        }
        return Result.error("充值失败");
    }

    /**
     * 修改密码（不需要原密码）
     *
     * @param newPassword 新密码
     * @param session     HTTP 会话
     * @return 修改结果
     */
    @PostMapping("/changePassword")
    public Result changePassword(@RequestParam String newPassword, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return Result.error("未登录");
        if (newPassword == null || newPassword.length() < 3) return Result.error("密码长度不能少于3位");

        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setPassword(org.springframework.util.DigestUtils.md5DigestAsHex(newPassword.getBytes()));
        if (userService.updateUserInfo(updateUser)) {
            return Result.success("密码修改成功");
        }
        return Result.error("修改失败");
    }

    /**
     * 用户登出
     * <p>
     * 从会话中移除用户信息，完成退出登录。
     * </p>
     *
     * @param session HTTP 会话
     * @return 登出成功返回提示信息
     */
    @PostMapping("/logout")
    public Result logout(HttpSession session) {
        session.removeAttribute("user");
        return Result.success("退出成功");
    }
}