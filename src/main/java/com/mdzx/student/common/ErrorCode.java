package com.mdzx.student.common;

/**
 * ErrorCode。
 * <p>提供跨业务模块共用的基础类型或规则。</p>
 */
public enum ErrorCode {
    PARAM_INVALID(40001, "参数校验失败"),
    UNAUTHORIZED(40100, "未登录或登录已过期"),
    FORBIDDEN(40300, "当前账号没有该操作权限"),
    NOT_FOUND(40401, "资源不存在"),
    SID_DUPLICATED(40901, "学号已存在"),
    INVALID_STATE(40902, "当前状态不允许该操作"),
    SERVER_ERROR(50000, "服务器内部错误");

    private final int code;
    private final String defaultMessage;

    ErrorCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    /** 获取可供接口返回的数字错误码。 */
    public int getCode() {
        return code;
    }

    /** 获取该错误码对应的默认提示文本。 */
    public String getDefaultMessage() {
        return defaultMessage;
    }
}
