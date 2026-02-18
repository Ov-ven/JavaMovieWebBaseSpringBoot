package com.software.movie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.software.movie.entity.User;
import com.software.movie.mapper.UserMapper;
import com.software.movie.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.Calendar;
import java.util.Date;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Autowired
    private UserMapper userMapper;


    @Override
    public User login(String username, String password) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        User user = baseMapper.selectOne(queryWrapper);
        if (user != null) {
            // 对用户输入的密码也进行MD5哈希
            String hashedPasswordInput = DigestUtils.md5DigestAsHex(password.getBytes());

            // 比对用户输入的哈希密码和数据库中存储的哈希密码
            if (hashedPasswordInput.equals(user.getPassword())) {
                return user; // 密码匹配
            }
        }
        return null; // 用户不存在或密码不匹配
    }

    @Override
    public boolean register(User user) {
        if (checkUsername(user.getUsername())) {
            return false;
        }

        user.setPassword(DigestUtils.md5DigestAsHex(user.getPassword().getBytes()));
        user.setIsvip(0);
        user.setStatus(true);
        return userMapper.insert(user) > 0;
    }

    @Override
    public boolean checkUsername(String username) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        return userMapper.selectCount(wrapper) > 0;
    }

    @Override
    public User getByUsername(String username) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        return userMapper.selectOne(wrapper);
    }

    @Override
    public boolean updateUserInfo(User user) {
        return userMapper.updateById(user) > 0;
    }

    @Override
    public boolean upgradeToVip(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return false;
        }

        Calendar calendar = Calendar.getInstance();
        if (user.getVipExpireTime() != null && user.getVipExpireTime().after(new Date())) {
            calendar.setTime(user.getVipExpireTime());
        } else {
            calendar.setTime(new Date());
        }
        calendar.add(Calendar.MONTH, 1); // 增加1个月VIP

        user.setIsvip(1);
        user.setVipExpireTime(calendar.getTime());
        return userMapper.updateById(user) > 0;
    }
}