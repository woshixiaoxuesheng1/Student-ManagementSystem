package com.mdzx.student.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 学生模块的出参 */
public final class StudentVos {

    /** 工具类仅提供静态数据或方法，不需要创建实例。 */
    StudentVos() {
    }

    /** 学科标识、名称、满分和展示顺序。 */
    public record Subject(
            String key,
            String name,
            Integer fullScore,
            Integer sortNo
    ) {
    }

    /** 本学期考勤统计 */
    public record AttendanceStat(
            int present,
            int late,
            int absent,
            int leave,
            int termDays,
            double rate
    ) {
    }

    /** 列表行 */
    public record Item(
            Long id,
            String sid,
            String name,
            String gender,
            String grade,
            Integer gradeIndex,
            String classId,
            String className,
            Integer classNo,
            String status,
            String phone,
            String guardian,
            String guardianRelation,
            String guardianPhone,
            LocalDate birthday,
            LocalDate enrollmentDate,
            String address,
            String dorm,
            String note,
            BigDecimal total,
            Integer gradeRank,
            Map<String, BigDecimal> scores,
            AttendanceStat attendance
    ) {
    }

    /** 学生简要信息，供排行榜等轻量展示场景使用。 */
    public record Brief(
            Long id,
            String sid,
            String name,
            BigDecimal total
    ) {
    }

    /** 学生某一天的考勤日期和状态。 */
    public record AttendanceDay(
            LocalDate date,
            String status
    ) {
    }

    /** 详情：档案 + 成绩 + 考勤 + 年级平均分 */
    public record Detail(
            Item student,
            double gradeAverage,
            List<AttendanceDay> recentDays,
            List<Subject> subjects
    ) {
    }

    /** 班级下拉选项及班主任、容量等辅助信息。 */
    public record ClassOption(
            String classId,
            String className,
            String grade,
            Integer gradeIndex,
            Integer classNo,
            String headTeacher,
            Integer capacity
    ) {
    }

    /** 前端下拉框所需的全部选项 */
    public record Options(
            List<String> grades,
            List<ClassOption> classes,
            List<String> statuses,
            List<Subject> subjects
    ) {
    }
}
