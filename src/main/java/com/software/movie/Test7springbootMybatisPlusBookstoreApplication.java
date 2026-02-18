package com.software.movie;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan("com.software.movie.mapper")
@EnableCaching
public class Test7springbootMybatisPlusBookstoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(Test7springbootMybatisPlusBookstoreApplication.class, args);
    }

}
