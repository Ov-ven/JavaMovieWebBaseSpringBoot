package com.software.movie.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 * <p>通过 {@code @RestControllerAdvice} 统一拦截 Controller 层抛出的异常，
 * 将其转换为标准的 {@link Result} 响应格式返回给前端，避免将堆栈信息泄露给客户端。</p>
 *
 * <p>处理优先级（从高到低）：</p>
 * <ol>
 *   <li>{@link NotLoginException} → 401 未登录</li>
 *   <li>{@link BusinessException} → 400 业务校验失败</li>
 *   <li>{@link DuplicateKeyException} → 4001 数据库唯一索引冲突</li>
 *   <li>{@link RuntimeException} → 500 系统异常（兜底）</li>
 * </ol>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 捕获未登录异常，返回 401 响应。
     *
     * @param e 未登录异常
     * @return 包含 UNAUTHORIZED 状态码的 Result
     */
    @ExceptionHandler(NotLoginException.class)
    public Result handleNotLoginException(NotLoginException e) {
        return Result.error(ResultCode.UNAUTHORIZED, e.getMessage());
    }

    /**
     * 捕获业务异常（可预期的校验失败），返回 400 响应。
     *
     * @param e 业务异常
     * @return 包含 BAD_REQUEST 状态码的 Result
     */
    @ExceptionHandler(BusinessException.class)
    public Result handleBusinessException(BusinessException e) {
        return Result.error(ResultCode.BAD_REQUEST, e.getMessage());
    }

    /**
     * 捕获数据库唯一索引冲突异常（兜底防重），返回 4001 响应。
     * <p>当业务层未提前校验唯一性时，此方法作为最后一道防线拦截数据库层面的重复冲突。</p>
     *
     * @param e 数据库唯一索引冲突异常
     * @return 包含 DUPLICATE_ACTION 状态码的 Result
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result handleDuplicateKeyException(DuplicateKeyException e) {
        log.warn("数据库唯一索引冲突：{}", e.getMessage());
        return Result.error(ResultCode.DUPLICATE_ACTION);
    }

    /**
     * 兜底异常处理器：捕获其他未处理的 RuntimeException，返回 500 响应。
     * <p>记录完整异常堆栈到日志，但仅返回通用错误消息给客户端。</p>
     *
     * @param e 运行时异常
     * @return 包含 SYSTEM_ERROR 状态码的 Result
     */
    @ExceptionHandler(RuntimeException.class)
    public Result handleRuntimeException(RuntimeException e) {
        log.error("系统异常：", e);
        return Result.error(ResultCode.SYSTEM_ERROR);
    }
}
