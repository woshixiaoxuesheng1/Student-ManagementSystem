package com.mdzx.student.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mdzx.student.common.ApiResponse;
import com.mdzx.student.dto.AuthDtos;
import com.mdzx.student.service.AuthService;
import com.mdzx.student.vo.AuthVos;

import jakarta.validation.Valid;

/**
 * AuthController。
 * <p>接收并校验 HTTP 请求，调用业务服务并返回统一接口数据。</p>
 */
@RestController// 全局映射请求路径 @Controller 和 @ResponseBody 主要用于开发REST API 所以简写成@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 登录请求：无需令牌 */
    @PostMapping("/login")
    public ApiResponse<AuthVos.LoginResult> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    /** 当前登录人：前端刷新页面后用它恢复身份
     * <p>@GetMapping查询当前用户信息</p>*/
    @GetMapping("/me")
    public ApiResponse<AuthVos.UserInfo> me() {
        return ApiResponse.ok(authService.currentUser());
    }

    /** 无状态令牌：真正的退出发生在客户端（丢弃 token） */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.ok();
    }

    /** 账号列表：仅系统管理员 */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")// 进行方法前权限校验
    public ApiResponse<List<AuthVos.UserItem>> users() {
        return ApiResponse.ok(authService.listUsers());
    }
}
/**
*<p>@GetMapping 查询数据请求</p>
 * <p>@PostMapping 新增数据,提交请求</p>
 * <p>@PutMapping 修改数据</p>
 * <p>@DeleteMapping 删除数据</p>
 * */