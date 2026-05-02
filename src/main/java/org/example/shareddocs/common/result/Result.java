package org.example.shareddocs.common.result;

import lombok.Data;

/**
 * 统一响应结果
 */
@Data
public class Result<T> {
    
    private Integer code;
    
    private String message;
    
    private T data;
    
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("成功");
        return result;
    }
    
    /**
     * 成功响应（无数据）
     */
    public static Result<Void> successVoid() {
        Result<Void> result = new Result<>();
        result.setCode(200);
        result.setMessage("成功");
        return result;
    }
    
    /**
     * 成功响应（带消息）
     */
    public static Result<Void> successVoid(String message) {
        Result<Void> result = new Result<>();
        result.setCode(200);
        result.setMessage(message);
        return result;
    }
    
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("成功");
        result.setData(data);
        return result;
    }
    
    public static <T> Result<T> success(String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage(message);
        result.setData(data);
        return result;
    }
    
    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }
    
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
