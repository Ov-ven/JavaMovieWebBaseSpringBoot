package com.software.movie.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.software.movie.entity.Person;
import com.software.movie.mapper.PersonMapper;
import com.software.movie.service.PersonService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 主创人员服务实现类。
 * 提供演员/导演等主创人员的信息查询功能。
 */
@Service
public class PersonServiceImpl extends ServiceImpl<PersonMapper, Person> implements PersonService {

    /**
     * 批量获取主创人员信息
     *
     * @param personIds 主创ID列表
     * @return 主创人员列表，空列表时返回空集合
     */
    @Override
    public List<Person> getPersonsByIds(List<Long> personIds) {
        if (personIds == null || personIds.isEmpty()) {
            return Collections.emptyList();
        }
        return baseMapper.selectBatchIds(personIds);
    }

    /**
     * 获取单个主创人员详情
     *
     * @param personId 主创ID
     * @return 主创人员实体，不存在时返回 null
     */
    @Override
    public Person getPersonById(Long personId) {
        return baseMapper.selectById(personId);
    }
}
