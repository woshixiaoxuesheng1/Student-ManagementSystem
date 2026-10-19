package com.mdzx.student.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.mdzx.student.common.BizException;
import com.mdzx.student.common.ErrorCode;
import com.mdzx.student.common.Role;
import com.mdzx.student.domain.ClassInfo;
import com.mdzx.student.domain.SysUser;
import com.mdzx.student.dto.AuthDtos;
import com.mdzx.student.mapper.ClassMapper;
import com.mdzx.student.mapper.UserMapper;
import com.mdzx.student.security.AuthContext;
import com.mdzx.student.security.JwtTokenProvider;
import com.mdzx.student.security.LoginUser;
import com.mdzx.student.service.AuthService;
import com.mdzx.student.vo.AuthVos;

/**
 * AuthService。
 * <p>实现对应业务流程，组合数据访问、校验与权限规则。</p>
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final ClassMapper classMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    public AuthServiceImpl(UserMapper userMapper, ClassMapper classMapper, PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider) {
        this.userMapper = userMapper;
        this.classMapper = classMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    /** 校验账号密码后签发令牌，并返回当前账号信息。 */
    @Override
    public AuthVos.LoginResult login(AuthDtos.LoginRequest request) {
        // 根据前端传来的参数查询数据库 校验密码和用户名是否正确
        SysUser user = userMapper.selectByUsername(request.username().trim());
        // 将传入的密码和是数据库的密码进行比较
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "登录名或密码不正确");
        }
        // 判断该用户是否能够使用 Boolean.TRUE.equals(user.getEnabled())返回false !false = true 表示该账户被停用
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BizException(ErrorCode.FORBIDDEN, "该账号已被停用，请联系系统管理员");
        }
        // 查询该用户可操作的班级列表 并根据编号查询班级名称
        List<String> classIds = userMapper.selectClassIds(user.getId());
        List<String> classNames = classNamesOf(classIds);
        // 记录用户最新登录时间
        LoginUser loginUser = new LoginUser(user, classIds, classNames);
        userMapper.updateLastLogin(user.getId());
        // 调用JWT生成令牌
        return new AuthVos.LoginResult(tokenProvider.createToken(loginUser), tokenProvider.getExpireSeconds(),
                toUserInfo(loginUser));
    }

    /** 返回当前登录用户的基本信息和数据范围。 */
    @Override
    public AuthVos.UserInfo currentUser() {
        return toUserInfo(AuthContext.require());
    }

    /** 查询全部账号并组装角色及负责班级信息。 */
    @Override
    public List<AuthVos.UserItem> listUsers() {
        List<SysUser> users = userMapper.selectAll();
        Map<Long, List<String>> classNamesById = new LinkedHashMap<>();
        if (!users.isEmpty()) {
            List<Long> ids = users.stream().map(SysUser::getId).toList();
            Map<String, String> nameByClassId = new LinkedHashMap<>();
            for (ClassInfo info : classMapper.selectAll(null, null)) {
                nameByClassId.put(info.getClassId(), info.getClassName());
            }
            for (String pair : userMapper.selectClassIdsByUserIds(ids)) {
                String[] parts = pair.split(":", 2);
                Long userId = Long.valueOf(parts[0]);
                classNamesById.computeIfAbsent(userId, key -> new ArrayList<>())
                        .add(nameByClassId.getOrDefault(parts[1], parts[1]));
            }
        }
        return users.stream()
                .map(user -> new AuthVos.UserItem(
                        user.getId(),
                        user.getUsername(),
                        user.getRealName(),
                        user.getRole(),
                        Role.valueOf(user.getRole()).getLabel(),
                        user.getPhone(),
                        Boolean.TRUE.equals(user.getEnabled()),
                        user.getLastLoginAt(),
                        classNamesById.getOrDefault(user.getId(), List.of())))
                .toList();
    }

    /** 根据班级编号查询班级名称。 */
    private List<String> classNamesOf(List<String> classIds) {
        if (classIds.isEmpty()) {
            return List.of();
        }
        return classMapper.selectAll(null, null).stream()
                .filter(info -> classIds.contains(info.getClassId()))
                .map(ClassInfo::getClassName)
                .toList();
    }

    /** 将认证用户转换为当前用户接口数据。 */
    private AuthVos.UserInfo toUserInfo(LoginUser user) {
        Role role = user.getRole();
        return new AuthVos.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                role.name(),
                role.getLabel(),
                // 判断角色的权限
                role == Role.ADMIN,
                role == Role.TEACHER,
                role == Role.VIEWER,
                null,
                user.getClassIds(),
                user.getClassNames());
    }
}
