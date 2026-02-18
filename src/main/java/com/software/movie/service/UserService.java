package com.software.movie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.software.movie.entity.User;

public interface UserService extends IService<User> {
    User login(String username, String password);

    boolean register(User user);

    boolean checkUsername(String username);

    User getByUsername(String username);

    boolean updateUserInfo(User user);

    boolean upgradeToVip(Long userId);
}