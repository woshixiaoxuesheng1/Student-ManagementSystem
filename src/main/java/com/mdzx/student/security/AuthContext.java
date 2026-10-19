package com.mdzx.student.security;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.mdzx.student.common.BizException;
import com.mdzx.student.common.ErrorCode;
import com.mdzx.student.common.Role;

/** 取当前登录用户与数据范围，业务层用它做行级权限 */
public final class AuthContext {


    /** 当前认证上下文工具类不允许创建实例。 */
    private AuthContext() {
    }

    /** 获取当前请求中的登录用户；未认证时返回空值。 */
    public static Optional<LoginUser> current() {
        // 从前端保存的token中获取当前登录角色信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser user)) {
            return Optional.empty();
        }
        return Optional.of(user);
    }

    /** 获取当前登录用户；未认证时抛出未登录异常。 */
    public static LoginUser require() {
        return current().orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED, "未登录或登录已过期"));
    }

    /**
     * 查询用的班级范围：
     * 班主任 → 自己负责的班级；管理员与只读账号 → null（不限）。
     */
    public static List<String> scopedClassIds() {
        LoginUser user = require();
        return user.getRole() == Role.TEACHER ? user.getClassIds() : null;
    }

    /** 判断当前用户是否为系统管理员。 */
    public static boolean isAdmin() {
        return current().map(user -> user.getRole() == Role.ADMIN).orElse(false);
    }

    /** 判断当前用户是否为班主任。 */
    public static boolean isTeacher() {
        return current().map(user -> user.getRole() == Role.TEACHER).orElse(false);
    }

    /** 班主任是否可以操作某个班级的数据 */
    public static boolean canAccessClass(String classId) {
        List<String> scoped = scopedClassIds();
        return scoped == null || scoped.contains(classId);
    }
}
