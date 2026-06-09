package com.software.movie.config;

import com.software.movie.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类。
 * <p>实现 {@link WebMvcConfigurer} 接口，注册登录拦截器 {@link LoginInterceptor}，
 * 并配置需要排除登录校验的路径（静态资源、公开页面、公开 API、外部回调等）。</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 注册拦截器并配置拦截/排除路径。
     * <p>拦截所有请求（{@code /**}），但排除以下路径：</p>
     * <ul>
     *   <li>静态资源：/css/**、/js/**、/image/**</li>
     *   <li>公开页面：首页、登录页、注册页、排行榜、影人详情、电影详情、VIP页面、图表页</li>
     *   <li>公开 API：登录、注册、用户名校验、电影浏览、评论列表</li>
     *   <li>外部回调：支付宝异步通知</li>
     * </ul>
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // ==================== 静态资源 ====================
                        "/css/**",
                        "/js/**",
                        "/image/**",

                        // ==================== 页面（无需登录） ====================
                        "/",                    // 首页
                        "/login",               // 登录页
                        "/register",            // 注册页
                        "/rank/**",             // 排行榜页（/rank/hot, /rank/top）
                        "/person/**",           // 影人详情页
                        "/detail/**",           // 电影详情页（内部自行判断登录态）
                        "/vip/**",              // VIP页面（内部自行判断登录态）
                        "/charts",              // 图表页

                        // ==================== API（无需登录） ====================
                        "/api/user/login",      // 登录接口
                        "/api/user/register",   // 注册接口
                        "/api/user/checkUsername", // 用户名校验
                        "/api/movie/**",        // 电影浏览相关接口
                        "/api/comment/list",    // 评论列表（公开浏览）

                        // ==================== 外部回调 ====================
                        "/payment/notify"       // 支付宝异步通知回调
                );
    }
}
