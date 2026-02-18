package com.software.movie.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.software.movie.entity.Person;
import com.software.movie.mapper.PersonMapper;
import com.software.movie.service.PersonService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class PersonServiceImpl extends ServiceImpl<PersonMapper, Person> implements PersonService {
    @Override
    public List<Person> getPersonsByIds(List<Long> personIds) {
        if (personIds == null || personIds.isEmpty()) {
            return Collections.emptyList();
        }
        return baseMapper.selectBatchIds(personIds);
    }

    @Override
    public Person getPersonById(Long personId) {
        return baseMapper.selectById(personId);
    }
}