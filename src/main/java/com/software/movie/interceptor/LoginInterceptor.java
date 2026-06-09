package com.software.movie.interceptor;

import com.software.movie.common.NotLoginException;
import com.software.movie.common.UserContext;
import com.software.movie.entity.User;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录拦截器：
 * 1. preHandle  —— 从 Session 取出用户，存入 UserContext；未登录则抛出异常。
 * 2. afterCompletion —— 清理 ThreadLocal，防止内存泄漏与线程复用数据污染。
 */
public class LoginInterceptor implements HandlerInterceptor {

    /**
     * 请求前置处理：从Session中获取用户并存入UserContext。
     *
     * @param request  HTTP请求
     * @param response HTTP响应
     * @param handler  处理器
     * @return true-放行请求
     * @throws NotLoginException 未登录时抛出
     */
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        User user = (User) request.getSession().getAttribute("user");
        if (user == null) {
            throw new NotLoginException();
        }
        UserContext.setUser(user);
        return true;
    }

    /**
     * 请求完成后置处理：清理ThreadLocal中的用户信息，防止内存泄漏。
     *
     * @param request  HTTP请求
     * @param response HTTP响应
     * @param handler  处理器
     * @param ex       异常（可为null）
     */
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        UserContext.removeUser();
    }
}
