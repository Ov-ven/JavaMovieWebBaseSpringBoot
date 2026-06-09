package com.software.movie.common;

/**
 * 未登录异常。
 * <p>当用户访问需要登录的接口但未提供有效凭证时，由 {@code LoginInterceptor} 抛出，
 * 由 {@link GlobalExceptionHandler} 统一捕获并返回 401 响应。</p>
 */
public class NotLoginException extends RuntimeException {

    /**
     * 构造默认未登录异常，使用默认消息"未登录或会话已过期"。
     */
    public NotLoginException() {
        super("未登录或会话已过期");
    }

    /**
     * 构造自定义消息的未登录异常。
     *
     * @param message 自定义错误消息
     */
    public NotLoginException(String message) {
        super(message);
    }
}
