package com.software.movie.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 秒杀压测页面控制器。
 * <p>返回 Thymeleaf 模板页面，无需鉴权（/test/** 已在 WebMvcConfig 白名单中）。</p>
 */
@Controller
public class SecKillPageController {

    @GetMapping("/test/seckill")
    public String seckillTestPage() {
        return "seckill-test";
    }
}
