package com.mdzx.student.vo;


import java.math.BigDecimal;

/** 班级模块的出参 */
public final class ClassVos {

    /** 工具类仅提供静态数据或方法，不需要创建实例。 */
    ClassVos() {
    }

    /** 班级概况及人数、成绩、考勤和优秀学生信息。 */
    public record Summary(
            String classId,
            String className,
            String grade,
            Integer gradeIndex,
            Integer classNo,
            String headTeacher,
            Integer capacity,
            int count,
            int activeCount,
            BigDecimal avgTotal,
            double attendanceRate,
            String topStudentName,
            BigDecimal topStudentTotal
    ) {
    }
}
