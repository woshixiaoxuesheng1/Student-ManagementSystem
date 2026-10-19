package com.mdzx.student.service.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mdzx.student.mapper.model.AttendanceStatRow;
import com.mdzx.student.vo.StudentVos;

/**
 * ValueAssemblerTest。
 * <p>定义业务服务对外提供的操作。</p>
 */
class ValueAssemblerTest {

    private final ValueAssembler assembler = new ValueAssembler();

    @Test
    @DisplayName("出勤率分子含迟到、不含请假与缺勤")
    void attendanceRate() {
        StudentVos.AttendanceStat stat = assembler.toAttendanceStat(50, 4, 2, 6);
        assertThat(stat.termDays()).isEqualTo(62);
        assertThat(stat.rate()).isEqualTo(ValueAssembler.round4(54d / 62));
    }

    @Test
    @DisplayName("没有考勤记录时返回 0 而不是 NaN")
    void emptyAttendance() {
        StudentVos.AttendanceStat stat = assembler.toAttendanceStat(null);
        assertThat(stat.termDays()).isZero();
        assertThat(stat.rate()).isZero();
        assertThat(Double.isNaN(stat.rate())).isFalse();
    }

    @Test
    @DisplayName("null 计数视为 0")
    void nullCounts() {
        AttendanceStatRow row = new AttendanceStatRow();
        row.setPresent(10);
        StudentVos.AttendanceStat stat = assembler.toAttendanceStat(row);
        assertThat(stat.present()).isEqualTo(10);
        assertThat(stat.late()).isZero();
        assertThat(stat.rate()).isEqualTo(1.0d);
    }
}
