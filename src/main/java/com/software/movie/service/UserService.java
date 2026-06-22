package com.software.movie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.software.movie.entity.User;

/**
 * 用户服务接口。
 * 提供用户注册、登录、信息更新及VIP升级等功能。
 */
public interface UserService extends IService<User> {

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码（明文，内部进行 MD5 哈希比对）
     * @return 登录成功返回用户实体，失败返回 null
     */
    User login(String username, String password);

    /**
     * 用户注册
     *
     * @param user 用户实体（需包含 username 和 password）
     * @return 注册是否成功（用户名已存在时返回 false）
     */
    boolean register(User user);

    /**
     * 检查用户名是否已存在
     *
     * @param username 用户名
     * @return 用户名是否已被占用
     */
    boolean checkUsername(String username);

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户实体，不存在时返回 null
     */
    User getByUsername(String username);

    /**
     * 更新用户信息
     *
     * @param user 用户实体
     * @return 操作是否成功
     */
    boolean updateUserInfo(User user);

    /**
     * 判断用户是否为有效VIP。
     * <p>读时判断：如果 isvip=1 但 vip_expire_time 已过期，会立即将数据库中 isvip 重置为 0。</p>
     *
     * @param userId 用户ID
     * @return true=有效VIP，false=非VIP或已过期
     */
    boolean isVip(Long userId);

    /**
     * 升级VIP（默认1个月）
     *
     * @param userId 用户ID
     * @return 操作是否成功
     */
    boolean upgradeToVip(Long userId);

    /**
     * 升级VIP，指定月数
     *
     * @param userId 用户ID
     * @param months VIP月数
     * @return 操作是否成功
     */
    boolean upgradeToVip(Long userId, int months);

    /**
     * 余额充值
     *
     * @param userId 用户ID
     * @param amount 充值金额（正数）
     * @return 操作是否成功
     */
    boolean recharge(Long userId, Double amount);

    /**
     * 余额扣减（余额不足时抛异常）
     *
     * @param userId 用户ID
     * @param amount 扣减金额（正数）
     * @return 操作是否成功
     */
    boolean deductBalance(Long userId, Double amount);

    /**
     * 余额退款（原子操作）
     *
     * @param userId 用户ID
     * @param amount 退款金额（正数）
     * @return 操作是否成功
     */
    boolean refundBalance(Long userId, Double amount);
}
