package com.software.movie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.software.movie.entity.Person;

import java.util.List;

/**
 * 主创人员服务接口。
 * 提供演员/导演等主创人员的信息查询功能。
 */
public interface PersonService extends IService<Person> {

    /**
     * 批量获取主创人员信息
     *
     * @param personIds 主创ID列表
     * @return 主创人员列表
     */
    List<Person> getPersonsByIds(List<Long> personIds);

    /**
     * 获取单个主创人员详情
     *
     * @param personId 主创ID
     * @return 主创人员实体，不存在时返回 null
     */
    Person getPersonById(Long personId);
}
