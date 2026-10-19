package com.mdzx.student.common;

/**
 * 前端统一响应结果：{ code, message, data }。
 * code = 0 表示成功，其余为业务错误码。
 */
/** 接口统一响应的数据结构，包含业务码、提示信息和响应数据。 */
public record ApiResponse<T>(int code, String message, T data) {

    /** 创建包含数据的成功响应。 */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "ok", data);
    }

    /** 创建不带数据体的成功响应。 */
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(0, "ok", null);
    }

    /** 创建包含错误码和提示信息的失败响应。 */
    public static <T> ApiResponse<T> fail(ErrorCode code, String message) {
        return new ApiResponse<>(code.getCode(), message, null);
    }
}
