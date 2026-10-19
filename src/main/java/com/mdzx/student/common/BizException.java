package com.mdzx.student.common;

/**全局异常处理*/
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;

    /** 使用错误码默认提示创建业务异常。 */
    public BizException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    /** 使用错误码和自定义提示创建业务异常。 */
    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /** 获取该异常对应的业务错误码。 */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
