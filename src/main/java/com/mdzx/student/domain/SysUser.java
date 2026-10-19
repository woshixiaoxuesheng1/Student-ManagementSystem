package com.mdzx.student.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;


/**
 * SysUser。
 * <p>表示系统中的持久化业务实体及其字段。</p>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class SysUser {

    private Long id;
    private String username;
    private String password;
    private String realName;
    private String role;
    private String phone;
    private Boolean enabled;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
