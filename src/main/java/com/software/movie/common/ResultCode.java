package com.software.movie.common;

/**
 * 统一错误码枚举。
 * <p>规则：2xx 成功，4xx 客户端错误，5xx 服务端错误，4xxx/5xxx 为细分业务码。</p>
 * <p>配合 {@link Result} 使用，可通过 {@code Result.error(ResultCode.XXX)} 快速构建错误响应。</p>
 */
public enum ResultCode {

    /** 操作成功 */
    SUCCESS(200, "成功"),
    /** 未登录或会话已过期 */
    UNAUTHORIZED(401, "未登录或会话已过期"),
    /** 请求参数错误 */
    BAD_REQUEST(400, "请求参数错误"),
    /** 服务器内部错误 */
    SYSTEM_ERROR(500, "服务器开小差了"),
    /** 重复操作（如重复下单、重复购买） */
    DUPLICATE_ACTION(4001, "您已购买该影片或请勿重复下单");

    /** 状态码 */
    private final int code;
    /** 提示消息 */
    private final String message;

    /**
     * 构造结果码枚举。
     *
     * @param code    状态码
     * @param message 提示消息
     */
    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 获取状态码。
     *
     * @return 状态码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取提示消息。
     *
     * @return 提示消息
     */
    public String getMessage() {
        return message;
    }
}
