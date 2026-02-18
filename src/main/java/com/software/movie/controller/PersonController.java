package com.software.movie.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.software.movie.entity.Movie;
import com.software.movie.entity.Person;
import com.software.movie.service.MovieService;
import com.software.movie.service.PersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/person")
public class PersonController {

    @Autowired
    private PersonService personService;

    @Autowired
    private MovieService movieService;


    // PersonController.java

    @GetMapping("/{id}")
    public String getPersonDetail(@PathVariable("id") Long personId, Model model) {
        // 1. 获取主创信息
        Person person = personService.getPersonById(personId);
        if (person == null) return "error/404";
        model.addAttribute("person", person);

        // 2. 获取电影列表
        List<Movie> movies = movieService.getMoviesByPerson(personId);
        model.addAttribute("movies", movies);

        // 3. 修复错误：添加 header 需要的属性
        model.addAttribute("queryType", null);
        model.addAttribute("queryRegion", null);
        model.addAttribute("queryKeyword", null);
        model.addAttribute("querySort", null);
        model.addAttribute("currentPage", 1);

        // 创建一个空分页对象来满足 header 的需求
        IPage<Movie> dummyMoviePage = new Page<>(1, 12);
        dummyMoviePage.setRecords(Collections.emptyList());
        dummyMoviePage.setTotal(0);
        model.addAttribute("moviePage", dummyMoviePage);

        return "person";
    }
}
