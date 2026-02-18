package com.software.movie.common;

import lombok.Data;
import java.io.Serializable;

@Data
public class Result implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private Integer code;   // 状态码
    private String msg;     // 消息
    private Object data;    // 返回数据

    // 成功静态方法
    public static Result success() {
        Result result = new Result();
        result.success=true;
        result.setCode(200);
        result.setMsg("成功");
        return result;
    }

    public static Result success(Object data) {
        Result result = new Result();
        result.success=true;
        result.setCode(200);
        result.setMsg("成功");
        result.setData(data);
        return result;
    }

    public static Result success(String msg, Object data) {
        Result result = new Result();
        result.success=true;
        result.setCode(200);
        result.setMsg(msg);
        result.setData(data);
        return result;
    }

    // 错误静态方法
    public static Result error(String msg) {
        Result result = new Result();
        result.success=false;
        result.setMsg(msg);
        return result;
    }

    public static Result error(Integer code, String msg) {
        Result result = new Result();
        result.success=false;
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }


}