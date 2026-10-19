package com.mdzx.student.service.support;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.mdzx.student.domain.Subject;
import com.mdzx.student.mapper.model.AttendanceStatRow;
import com.mdzx.student.mapper.model.StudentRow;
import com.mdzx.student.vo.StudentVos;

/** 把 mapper 行对象组装成接口出参，并统一派生口径 */
@Component
public class ValueAssembler {

    /** 出勤率 =（出勤 + 迟到）/ 本学期教学日 */
    public StudentVos.AttendanceStat toAttendanceStat(AttendanceStatRow row) {
        return toAttendanceStat(
                value(row == null ? null : row.getPresent()),
                value(row == null ? null : row.getLate()),
                value(row == null ? null : row.getAbsent()),
                value(row == null ? null : row.getLeave()));
    }

    /** 根据考勤次数计算并组装考勤统计。 */
    public StudentVos.AttendanceStat toAttendanceStat(int present, int late, int absent, int leave) {
        int termDays = present + late + absent + leave;
        double rate = termDays == 0 ? 0d : (double) (present + late) / termDays;
        return new StudentVos.AttendanceStat(present, late, absent, leave, termDays, round4(rate));
    }

    /** 将学生查询行及成绩组装为学生列表项。 */
    public StudentVos.Item toItem(StudentRow row, Map<String, BigDecimal> scores) {
        return new StudentVos.Item(
                row.getId(),
                row.getSid(),
                row.getName(),
                row.getGender(),
                row.getGrade(),
                row.getGradeIndex(),
                row.getClassId(),
                row.getClassName(),
                row.getClassNo(),
                row.getStatus(),
                row.getPhone(),
                row.getGuardian(),
                row.getGuardianRelation(),
                row.getGuardianPhone(),
                row.getBirthday(),
                row.getEnrollmentDate(),
                row.getAddress(),
                row.getDorm(),
                row.getNote(),
                nullToZero(row.getTotal()),
                row.getGradeRank(),
                scores,
                toAttendanceStat(
                        value(row.getPresent()),
                        value(row.getLate()),
                        value(row.getAbsent()),
                        value(row.getLeave())));
    }

    /** 将学科实体转换为接口返回的学科选项。 */
    public List<StudentVos.Subject> toSubjects(List<Subject> subjects) {
        return subjects.stream()
                .map(s -> new StudentVos.Subject(s.getSubjectKey(), s.getSubjectName(), s.getFullScore(), s.getSortNo()))
                .collect(Collectors.toList());
    }

    /** 将可空整数转换为零或对应数值。 */
    public static int value(Integer number) {
        return number == null ? 0 : number;
    }

    /** 将 null 数值统一转换为零。 */
    public static BigDecimal nullToZero(BigDecimal number) {
        return number == null ? BigDecimal.ZERO : number;
    }

    /** 将比例值四舍五入到小数点后四位。 */
    public static double round4(double value) {
        return Math.round(value * 10000d) / 10000d;
    }
}
