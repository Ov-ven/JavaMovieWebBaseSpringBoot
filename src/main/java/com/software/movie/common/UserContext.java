package com.software.movie.common;

import com.software.movie.entity.User;

/**
 * 基于 ThreadLocal 的用户上下文持有者。
 * <p>用于在同一线程内（如 Controller → Service 调用链）共享当前登录用户信息。
 * 由拦截器在请求进入时设置，请求结束时清除。</p>
 *
 * <p>典型使用流程：</p>
 * <ol>
 *   <li>LoginInterceptor 在 preHandle 中调用 {@link #setUser(User)}</li>
 *   <li>Service 层通过 {@link #getUser()} 获取当前用户</li>
 *   <li>LoginInterceptor 在 afterCompletion 中调用 {@link #removeUser()} 防止内存泄漏</li>
 * </ol>
 */
public class UserContext {

    private static final ThreadLocal<User> USER_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 设置当前线程的登录用户。
     *
     * @param user 当前登录用户对象
     */
    public static void setUser(User user) {
        USER_THREAD_LOCAL.set(user);
    }

    /**
     * 获取当前线程的登录用户。
     *
     * @return 当前登录用户对象，未登录时返回 null
     */
    public static User getUser() {
        return USER_THREAD_LOCAL.get();
    }

    /**
     * 清除当前线程的用户上下文。
     * <p>必须在请求结束时调用（如拦截器的 afterCompletion），避免线程复用导致内存泄漏。</p>
     */
    public static void removeUser() {
        USER_THREAD_LOCAL.remove();
    }
}
