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

/**
 * 用户服务实现类。
 * 提供用户注册、登录验证、信息更新及VIP升级等功能。
 * 密码使用 MD5 哈希存储。
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Autowired
    private UserMapper userMapper;

    /**
     * 用户登录验证。
     * 根据用户名查询用户，并对输入密码进行 MD5 哈希后与数据库存储的哈希值比对。
     *
     * @param username 用户名
     * @param password 密码（明文）
     * @return 登录成功返回用户实体，用户名不存在或密码不匹配返回 null
     */
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

    /**
     * 用户注册。
     * 用户名已存在时返回 false；密码自动进行 MD5 哈希后存储。
     *
     * @param user 用户实体（需包含 username 和 password）
     * @return 注册是否成功
     */
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

    /**
     * 检查用户名是否已存在
     *
     * @param username 用户名
     * @return 用户名是否已被占用
     */
    @Override
    public boolean checkUsername(String username) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        return userMapper.selectCount(wrapper) > 0;
    }

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户实体，不存在时返回 null
     */
    @Override
    public User getByUsername(String username) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        return userMapper.selectOne(wrapper);
    }

    /**
     * 更新用户信息
     *
     * @param user 用户实体
     * @return 操作是否成功
     */
    @Override
    public boolean updateUserInfo(User user) {
        return userMapper.updateById(user) > 0;
    }

    /**
     * 判断用户是否为有效VIP。
     * <p>读时判断：如果 isvip=1 但 vip_expire_time 已过期，立即将数据库中 isvip 重置为 0。</p>
     *
     * @param userId 用户ID
     * @return true=有效VIP，false=非VIP或已过期
     */
    @Override
    public boolean isVip(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getIsvip() != 1) {
            return false;
        }
        // 检查是否过期
        if (user.getVipExpireTime() != null && user.getVipExpireTime().before(new Date())) {
            // 已过期，降级
            user.setIsvip(0);
            userMapper.updateById(user);
            return false;
        }
        return true;
    }

    /**
     * 升级VIP（默认1个月）
     *
     * @param userId 用户ID
     * @return 操作是否成功
     */
    @Override
    public boolean upgradeToVip(Long userId) {
        return upgradeToVip(userId, 1);
    }

    /**
     * 升级VIP，指定月数。
     * 若用户当前VIP未过期，则在到期时间基础上续期；否则从当前时间开始计算。
     *
     * @param userId 用户ID
     * @param months VIP月数
     * @return 操作是否成功（用户不存在时返回 false）
     */
    @Override
    public boolean upgradeToVip(Long userId, int months) {
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
        calendar.add(Calendar.MONTH, months);

        user.setIsvip(1);
        user.setVipExpireTime(calendar.getTime());
        return userMapper.updateById(user) > 0;
    }

    /**
     * 余额充值（原子操作）
     *
     * @param userId 用户ID
     * @param amount 充值金额（正数）
     * @return 操作是否成功
     */
    @Override
    public boolean recharge(Long userId, Double amount) {
        if (amount == null || amount <= 0) return false;
        return userMapper.addBalance(userId, new java.math.BigDecimal(String.valueOf(amount))) > 0;
    }

    /**
     * 余额扣减（原子操作，防并发超额扣减）
     *
     * @param userId 用户ID
     * @param amount 扣减金额（正数）
     * @return 操作是否成功
     * @throws BusinessException 余额不足时抛出
     */
    @Override
    public boolean deductBalance(Long userId, Double amount) {
        if (amount == null || amount <= 0) return false;
        int rows = userMapper.deductBalance(userId, new java.math.BigDecimal(String.valueOf(amount)));
        if (rows == 0) {
            throw new com.software.movie.common.BusinessException("余额不足或扣减失败");
        }
        return true;
    }

    /**
     * 余额退款（原子操作）
     *
     * @param userId 用户ID
     * @param amount 退款金额（正数）
     * @return 操作是否成功
     */
    @Override
    public boolean refundBalance(Long userId, Double amount) {
        if (amount == null || amount <= 0) return false;
        return userMapper.addBalance(userId, new java.math.BigDecimal(String.valueOf(amount))) > 0;
    }
}
