package com.mdzx.student.common;

import java.util.List;

/** 角色：权限边界集中在这里，避免各处硬编码字符串 */
public enum Role {

    /** 系统管理员：全校数据 + 全部写操作 + 系统设置 */
    ADMIN("系统管理员"),
    /** 班主任：只能看/管自己负责班级的数据 */
    TEACHER("班主任"),
    /** 校领导：全校数据只读 */
    VIEWER("只读账号");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    /** 返回角色对应的中文名称。 */
    public String getLabel() {
        return label;
    }

    /** 判断字符串是否为系统定义的角色。 */
    public static boolean isValid(String value) {
        for (Role role : values()) {
            if (role.name().equals(value)) {
                return true;
            }
        }
        return false;
    }

    /** 能改学籍的角色 */
    public static List<String> studentWriters() {
        return List.of(ADMIN.name(), TEACHER.name());
    }

    /** 能标记考勤的角色 */
    public static List<String> attendanceWriters() {
        return List.of(ADMIN.name(), TEACHER.name());
    }
}
