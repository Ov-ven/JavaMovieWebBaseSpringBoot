package com.software.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.software.movie.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单数据访问层接口。
 * 继承 MyBatis-Plus 的 BaseMapper，提供订单记录的 CRUD 操作。
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}