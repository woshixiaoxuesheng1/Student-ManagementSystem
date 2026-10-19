package com.mdzx.student.vo;


import java.time.LocalDate;
import java.util.List;

import com.mdzx.student.common.PageResult;
import com.mdzx.student.vo.StudentVos.AttendanceStat;

/**3. vo：View Object，视图对象
*重点：组织后端准备返回给前端展示的数据。
*例如查询学生时，后端可以使用 StudentVos.Item 组织学生姓名、班级名称、性别等展示字段，再返回给前端。*/
/** 考勤模块的出参 */
public final class AttendanceVos {

    /** 工具类仅提供静态数据或方法，不需要创建实例。 */
    AttendanceVos() {
    }

    /** 指定日期各考勤状态的人数。 */
    public record Counts(
            int present,
            int late,
            int absent,
            int leave
    ) {
    }

    /** 考勤列表顶部的日期、到校率和登记进度汇总。 */
    public record Summary(
            LocalDate date,
            boolean schoolDay,
            int total,
            Counts counts,
            double attendanceRate,
            int markedCount
    ) {
    }

    /** 考勤列表中的学生及当日、学期考勤信息。 */
    public record Row(
            Long id,
            String sid,
            String name,
            String gender,
            String grade,
            String classId,
            String className,
            String dayStatus,
            AttendanceStat term
    ) {
    }

    /** 考勤分页结果、当日汇总和近期上学日。 */
    public record Page(
            Summary summary,
            PageResult<Row> page,
            List<LocalDate> recentSchoolDays
    ) {
    }
}
