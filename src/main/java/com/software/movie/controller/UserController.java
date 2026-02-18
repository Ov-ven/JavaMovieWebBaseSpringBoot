package com.software.movie.controller;

import com.software.movie.common.Result;
import com.software.movie.entity.User;
import com.software.movie.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result login(@RequestBody User user, HttpSession session) {
        User loginUser = userService.login(user.getUsername(), user.getPassword());
        if (loginUser != null) {
            session.setAttribute("user", loginUser);
            return Result.success(loginUser);
        }
        return Result.error("用户名或密码错误");
    }

    @PostMapping("/register")
    public Result register(@RequestBody User user) {
        if (userService.register(user)) {
            return Result.success("注册成功");
        }
        return Result.error("用户名已存在");
    }

    @GetMapping("/checkUsername")
    public Result checkUsername(@RequestParam String username) {
        return Result.success(userService.checkUsername(username));
    }

    @GetMapping("/info")
    public Result getInfo(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            return Result.success(user);
        }
        return Result.error("未登录");
    }

    @PostMapping("/update")
    public Result updateInfo(@RequestBody User user, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error("未登录");
        }

        user.setId(currentUser.getId());
        if (userService.updateUserInfo(user)) {
            session.setAttribute("user", userService.getByUsername(currentUser.getUsername()));
            return Result.success("更新成功");
        }
        return Result.error("更新失败");
    }

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

    @PostMapping("/logout")
    public Result logout(HttpSession session) {
        session.removeAttribute("user");
        return Result.success("退出成功");
    }
}