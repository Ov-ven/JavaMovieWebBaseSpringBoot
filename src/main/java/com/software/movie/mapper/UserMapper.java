package com.software.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.software.movie.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

/**
 * 用户数据访问层接口。
 * 继承 MyBatis-Plus 的 BaseMapper，提供用户信息的 CRUD 操作。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 原子扣减余额（防并发超额扣减）
     *
     * @param userId 用户ID
     * @param amount 扣减金额
     * @return 受影响行数（0 表示余额不足）
     */
    int deductBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 原子增加余额
     *
     * @param userId 用户ID
     * @param amount 充值金额
     * @return 受影响行数
     */
    int addBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}