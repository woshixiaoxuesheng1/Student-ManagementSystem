package com.mdzx.student.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 概览页的出参 */
public final class DashboardVos {

    /** 工具类仅提供静态数据或方法，不需要创建实例。 */
    DashboardVos() {
    }

    /** 概览页指定日期的各类考勤人数。 */
    public record AttendanceCounts(
            int present,
            int late,
            int absent,
            int leave
    ) {
    }

    /** 概览页的学生、班级、考勤和成绩核心指标。 */
    public record Metrics(
            long totalStudents,
            long activeStudents,
            long suspendedStudents,
            long classCount,
            double avgClassSize,
            LocalDate attendanceDate,
            double attendanceRate,
            AttendanceCounts attendanceCounts,
            BigDecimal avgTotal,
            BigDecimal bestTotal,
            int fullScore,
            double deltaVsLastWeek
    ) {
    }

    /** 某一周的考勤趋势数据点。 */
    public record TrendPoint(
            int weekNo,
            String label,
            LocalDate startDate,
            double rate
    ) {
    }

    /** 概览页待办事项及其跳转信息。 */
    public record Todo(
            String id,
            String title,
            String detail,
            long count,
            String tone,
            String actionLabel,
            Map<String, String> actionQuery
    ) {
    }

    /** 按年级汇总的班级、学生、成绩和考勤数据。 */
    public record GradeSummary(
            String grade,
            long classCount,
            long totalStudents,
            long activeStudents,
            BigDecimal avgTotal,
            double attendanceRate
    ) {
    }

    /** 最近异动学生及其考勤概况。 */
    public record Move(
            Long id,
            String sid,
            String name,
            String grade,
            String className,
            String status,
            double attendanceRate
    ) {
    }

    /** 概览页展示所需的完整数据集合。 */
    public record Overview(
            String term,
            String schoolName,
            LocalDate generatedAt,
            Metrics metrics,
            List<TrendPoint> trend,
            List<Todo> todos,
            List<GradeSummary> grades,
            List<Move> recentMoves
    ) {
    }
}
