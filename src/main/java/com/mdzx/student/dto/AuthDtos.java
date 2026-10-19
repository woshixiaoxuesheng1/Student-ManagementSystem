package com.mdzx.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
/**
 * AuthDtos。
 * <p>定义接口接收的请求数据结构及校验规则。</p>
 * <p>入参接受前端传入的参数</p>
 */
public final class AuthDtos {

    /** 工具类仅提供静态数据或方法，不需要创建实例。 */
    AuthDtos() {
    }
    /** 用户登录时提交的账号和密码。 */
    public record LoginRequest(
            @NotBlank(message = "请输入登录名") @Size(max = 32) String username,
            @NotBlank(message = "请输入密码") @Size(max = 64) String password
    ) {
    }
}
