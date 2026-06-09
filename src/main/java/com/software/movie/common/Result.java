package com.software.movie.common;

import lombok.Data;
import java.io.Serializable;

/**
 * 统一响应结果封装类。
 * <p>所有 Controller 接口的返回值均使用此类包装，确保前端收到的 JSON 结构一致。
 * 提供基于状态码和 {@link ResultCode} 的多种静态工厂方法，分别用于成功和失败场景。</p>
 *
 * <pre>
 * 成功示例：{"success":true,"code":200,"msg":"成功","data":{...}}
 * 失败示例：{"success":false,"code":400,"msg":"参数错误","data":null}
 * </pre>
 */
@Data
public class Result implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 是否成功 */
    private boolean success;
    /** 状态码（200-成功，4xx-客户端错误，5xx-服务端错误） */
    private Integer code;
    /** 提示消息 */
    private String msg;
    /** 返回数据 */
    private Object data;

    /**
     * 构建成功响应（无数据）。
     *
     * @return 成功的 Result 对象，code=200
     */
    public static Result success() {
        Result result = new Result();
        result.success=true;
        result.setCode(200);
        result.setMsg("成功");
        return result;
    }

    /**
     * 构建成功响应（带数据）。
     *
     * @param data 响应数据
     * @return 成功的 Result 对象，code=200
     */
    public static Result success(Object data) {
        Result result = new Result();
        result.success=true;
        result.setCode(200);
        result.setMsg("成功");
        result.setData(data);
        return result;
    }

    /**
     * 构建成功响应（自定义消息和数据）。
     *
     * @param msg  提示消息
     * @param data 响应数据
     * @return 成功的 Result 对象，code=200
     */
    public static Result success(String msg, Object data) {
        Result result = new Result();
        result.success=true;
        result.setCode(200);
        result.setMsg(msg);
        result.setData(data);
        return result;
    }

    /**
     * 构建失败响应（仅消息，无状态码）。
     * <p>注意：此方法不会设置 code 字段，返回的 code 为 null。</p>
     *
     * @param msg 错误提示消息
     * @return 失败的 Result 对象，code 为 null
     */
    public static Result error(String msg) {
        Result result = new Result();
        result.success=false;
        result.setCode(500);
        result.setMsg(msg);
        return result;
    }

    /**
     * 构建失败响应（指定状态码和消息）。
     *
     * @param code 错误状态码
     * @param msg  错误提示消息
     * @return 失败的 Result 对象
     */
    public static Result error(Integer code, String msg) {
        Result result = new Result();
        result.success=false;
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }

    // ========== 基于 ResultCode 的重载方法 ==========

    /**
     * 基于 {@link ResultCode} 构建成功响应（无数据）。
     *
     * @param resultCode 结果码枚举
     * @return 成功的 Result 对象
     */
    public static Result success(ResultCode resultCode) {
        Result result = new Result();
        result.success = true;
        result.setCode(resultCode.getCode());
        result.setMsg(resultCode.getMessage());
        return result;
    }

    /**
     * 基于 {@link ResultCode} 构建成功响应（带数据）。
     *
     * @param resultCode 结果码枚举
     * @param data       响应数据
     * @return 成功的 Result 对象
     */
    public static Result success(ResultCode resultCode, Object data) {
        Result result = new Result();
        result.success = true;
        result.setCode(resultCode.getCode());
        result.setMsg(resultCode.getMessage());
        result.setData(data);
        return result;
    }

    /**
     * 基于 {@link ResultCode} 构建失败响应。
     *
     * @param resultCode 结果码枚举
     * @return 失败的 Result 对象
     */
    public static Result error(ResultCode resultCode) {
        Result result = new Result();
        result.success = false;
        result.setCode(resultCode.getCode());
        result.setMsg(resultCode.getMessage());
        return result;
    }

    /**
     * 基于 {@link ResultCode} 构建失败响应（自定义消息）。
     *
     * @param resultCode 结果码枚举
     * @param msg        自定义错误提示消息（覆盖枚举默认消息）
     * @return 失败的 Result 对象
     */
    public static Result error(ResultCode resultCode, String msg) {
        Result result = new Result();
        result.success = false;
        result.setCode(resultCode.getCode());
        result.setMsg(msg);
        return result;
    }
}
