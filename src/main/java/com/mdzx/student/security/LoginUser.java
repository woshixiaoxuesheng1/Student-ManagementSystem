package com.mdzx.student.security;

import java.util.Collection;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.mdzx.student.common.Role;
import com.mdzx.student.domain.SysUser;

/** 登录态：角色 + 负责班级，班主任的数据范围就来自这里 */
@Data
@AllArgsConstructor
public class LoginUser implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final String realName;
    private final Role role;
    private final boolean enabled;
    private final List<String> classIds;
    private final List<String> classNames;
    public LoginUser(SysUser user, List<String> classIds, List<String> classNames) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.realName = user.getRealName();
        this.role = Role.valueOf(user.getRole());
        this.enabled = Boolean.TRUE.equals(user.getEnabled());
        this.classIds = List.copyOf(classIds);
        this.classNames = List.copyOf(classNames);
    }

    /*public Long getId() {
        return id;
    }

    public String getRealName() {
        return realName;
    }

    public Role getRole() {
        return role;
    }

    public List<String> getClassIds() {
        return classIds;
    }

    public List<String> getClassNames() {
        return classNames;
    }*/

    /** 将用户角色转换为 Spring Security 授权项。 */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /** 获取密码。 */
    @Override
    public String getPassword() {
        return password;
    }

    /** 获取登录名。 */
    @Override
    public String getUsername() {
        return username;
    }

    /** 判断account non expired条件是否成立。 */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /** 判断account non locked条件是否成立。 */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /** 判断credentials non expired条件是否成立。 */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** 判断enabled条件是否成立。 */
    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
