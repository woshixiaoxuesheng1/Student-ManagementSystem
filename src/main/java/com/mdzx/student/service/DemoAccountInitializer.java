package com.mdzx.student.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mdzx.student.common.Role;
import com.mdzx.student.domain.ClassInfo;
import com.mdzx.student.domain.SysUser;
import com.mdzx.student.mapper.ClassMapper;
import com.mdzx.student.mapper.UserMapper;

/**
 * 演示账号初始化：sys_user 为空时创建四个账号，密码用 BCrypt 加密。
 * 口令写在代码里只是为了演示，生产环境应改为从配置或后台重置。
 */
/**
 * DemoAccountInitializer。
 * <p>定义业务服务对外提供的操作。</p>
 */
@Service
@DependsOn("demoDataService")
public class DemoAccountInitializer {

    private static final Logger log = LoggerFactory.getLogger(DemoAccountInitializer.class);

    private final UserMapper userMapper;
    private final ClassMapper classMapper;
    private final PasswordEncoder passwordEncoder;
    public DemoAccountInitializer(UserMapper userMapper, ClassMapper classMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.classMapper = classMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /** 仅在账号表为空时初始化演示账号。 */
    @EventListener(ApplicationReadyEvent.class)
    @Order(20)
    public void initAccounts() {
        try {
            if (userMapper.countAll() > 0) {
                return;
            }
            createAccounts();
            log.info("演示账号已创建：admin/admin123（管理员）、teacher1/teacher123（班主任）、viewer/viewer123（只读）");
        } catch (Exception ex) {
            log.warn("演示账号初始化失败（请确认已执行 db/schema.sql）：{}", ex.getMessage());
        }
    }

    /** 创建演示账号并关联负责班级。 */
    @Transactional
    public void createAccounts() {
        List<String> classIds = classMapper.selectAll(null, null).stream().map(ClassInfo::getClassId).toList();
        create("admin", "admin123", "袁一鸣", Role.ADMIN, "0571-8812 6601", List.of());
        create("teacher1", "teacher123", "陆敏华", Role.TEACHER, "13800001001",
                pick(classIds, "高一-1", "高一-2"));
        create("teacher2", "teacher123", "祁明远", Role.TEACHER, "13800001002",
                pick(classIds, "高三-1", "高三-2"));
        create("viewer", "viewer123", "沈立群", Role.VIEWER, "0571-8812 6600", List.of());
    }

    /** 校验请求数据并创建记录。 */
    private void create(String username, String rawPassword, String realName, Role role, String phone,
            List<String> classIds) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRealName(realName);
        user.setRole(role.name());
        user.setPhone(phone);
        user.setEnabled(true);
        userMapper.insert(user);
        if (!classIds.isEmpty()) {
            userMapper.insertUserClasses(user.getId(), classIds);
        }
    }

    /** 从已有班级中选出演示班级；无匹配项时返回空列表。 */
    private List<String> pick(List<String> existing, String... wanted) {
        List<String> result = new java.util.ArrayList<>();
        for (String classId : wanted) {
            if (existing.contains(classId)) {
                result.add(classId);
            }
        }
        return result;
    }
}
