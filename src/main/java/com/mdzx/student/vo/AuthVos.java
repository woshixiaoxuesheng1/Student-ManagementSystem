package com.mdzx.student.vo;



import java.time.LocalDateTime;
import java.util.List;

/** 登录与账号相关的出参(返回给前端得参数) */
public final class AuthVos {

    /** 工具类仅提供静态数据或方法，不需要创建实例。 */
    AuthVos() {
    }

    /** 当前登录人：前端据此显示身份并控制按钮可见性 */
    public record UserInfo(
            Long id,
            String username,
            String realName,
            String role,
            String roleLabel,
            boolean admin,
            boolean teacher,
            boolean viewer,
            String phone,
            List<String> classIds,
            List<String> classNames
    ) {
    }

    /** 登录成功后返回的令牌、有效期和用户信息。 */
    public record LoginResult(
            String token,
            long expiresIn,
            UserInfo user
    ) {
    }

    /** 账号列表（仅管理员可见） */
    public record UserItem(
            Long id,
            String username,
            String realName,
            String role,
            String roleLabel,
            String phone,
            boolean enabled,
            LocalDateTime lastLoginAt,
            List<String> classNames
    ) {
    }
}
