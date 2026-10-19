package com.mdzx.student.vo;


import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.mdzx.student.common.PageResult;
import com.mdzx.student.vo.StudentVos.Subject;

/** 成绩模块的出参 */
public final class GradeVos {

    /** 工具类仅提供静态数据或方法，不需要创建实例。 */
    GradeVos() {
    }

    /** 成绩列表中的学生成绩行，包含总分、排名和各科成绩。 */
    public record Row(
            Long id,
            String sid,
            String name,
            String gender,
            String grade,
            String classId,
            String className,
            String status,
            BigDecimal total,
            Integer gradeRank,
            Map<String, BigDecimal> scores
    ) {
    }

    /** 当前成绩查询范围内的统计指标。 */
    public record Stats(
            int studentCount,
            BigDecimal avgTotal,
            BigDecimal bestTotal,
            String bestStudentName,
            String bestClassName,
            int fullScore,
            int passLine,
            int excellentLine,
            double passRate,
            double excellentRate
    ) {
    }

    /** 成绩分页数据、统计指标及学科信息。 */
    public record Page(
            PageResult<Row> page,
            Stats stats,
            List<Subject> subjects
    ) {
    }
}
