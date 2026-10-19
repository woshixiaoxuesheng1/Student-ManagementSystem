package com.mdzx.student.common;

import java.util.List;

/** 与前端共用的一套枚举值（中文即接口取值） */
public final class Constants {

    /** 常量工具类不允许创建实例。 */
    private Constants() {
    }

    public static final List<String> GRADES = List.of("高一", "高二", "高三");
    public static final List<String> STATUSES = List.of("在读", "休学", "转学", "退学");
    public static final List<String> ATTENDANCE_STATUSES = List.of("出勤", "迟到", "缺勤", "请假");
    public static final String GENDER_MALE = "男";
    public static final String GENDER_FEMALE = "女";
    public static final String STATUS_ACTIVE = "在读";
    public static final String ATTENDANCE_PRESENT = "出勤";
    public static final String ATTENDANCE_LATE = "迟到";
    public static final String ATTENDANCE_ABSENT = "缺勤";
    public static final String ATTENDANCE_LEAVE = "请假";

    /** 前端「缺勤偏多」待办的阈值 */
    public static final int ABSENT_ALERT_THRESHOLD = 2;
    /** 及格率 / 优秀率按总分比例计算 */
    public static final double PASS_RATIO = 0.6;
    public static final double EXCELLENT_RATIO = 0.8;
}
