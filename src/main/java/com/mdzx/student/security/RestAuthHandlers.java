package com.mdzx.student.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdzx.student.common.ApiResponse;
import com.mdzx.student.common.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** 未登录 / 权限不足时也返回统一响应结构，前端可以直接弹提示 */
@Component
public class RestAuthHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /** 注入 JSON 序列化器，用于生成认证失败响应。 */
    public RestAuthHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 将未登录错误写成统一 JSON 响应。 */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        write(response, HttpServletResponse.SC_UNAUTHORIZED,
                ApiResponse.fail(ErrorCode.UNAUTHORIZED, "未登录或登录已过期，请重新登录"));
    }

    /** 将无权访问错误写成统一 JSON 响应。 */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        write(response, HttpServletResponse.SC_FORBIDDEN,
                ApiResponse.fail(ErrorCode.FORBIDDEN, "当前账号没有该操作权限"));
    }

    /** 设置 HTTP 状态并序列化统一响应体。 */
    private void write(HttpServletResponse response, int status, ApiResponse<Void> body) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
