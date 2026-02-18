package com.software.movie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.software.movie.entity.Person;

import java.util.List;

public interface PersonService extends IService<Person> {
    List<Person> getPersonsByIds(List<Long> personIds);
    Person getPersonById(Long personId); // 获取单个主创详情
}
