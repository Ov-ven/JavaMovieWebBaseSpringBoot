package com.software.movie.common;

/**
 * 业务异常。
 * <p>用于可预期的业务校验失败场景（如重复下单、电影不存在、库存不足等）。
 * 由 {@link GlobalExceptionHandler} 统一捕获并返回 {@link ResultCode#BAD_REQUEST} 响应。</p>
 */
public class BusinessException extends RuntimeException {

    /**
     * 构造业务异常。
     *
     * @param message 业务错误描述消息
     */
    public BusinessException(String message) {
        super(message);
    }
}
