package com.mdzx.student.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mdzx.student.common.BizException;
import com.mdzx.student.common.Constants;
import com.mdzx.student.common.ErrorCode;
import com.mdzx.student.common.PageResult;
import com.mdzx.student.common.SchoolCalendar;
import com.mdzx.student.dto.AttendanceDtos;
import com.mdzx.student.security.AuthContext;
import com.mdzx.student.mapper.AttendanceMapper;
import com.mdzx.student.mapper.StudentMapper;
import com.mdzx.student.mapper.model.AttendanceRow;
import com.mdzx.student.mapper.model.AttendanceStatRow;
import com.mdzx.student.service.AttendanceService;
import com.mdzx.student.service.support.ValueAssembler;
import com.mdzx.student.vo.AttendanceVos;

/**
 * AttendanceService。
 * <p>实现对应业务流程，组合数据访问、校验与权限规则。</p>
 */
@Service
public class    AttendanceServiceImpl implements AttendanceService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AttendanceMapper attendanceMapper;
    private final StudentMapper studentMapper;
    private final ValueAssembler assembler;
    public AttendanceServiceImpl(AttendanceMapper attendanceMapper, StudentMapper studentMapper,
            ValueAssembler assembler) {
        this.attendanceMapper = attendanceMapper;
        this.studentMapper = studentMapper;
        this.assembler = assembler;
    }

    /** 汇总指定日期考勤，并返回符合条件的学生分页数据。 */
    @Override
    public AttendanceVos.Page page(AttendanceDtos.Query query, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        LocalDate date = query.date() == null ? LocalDate.now() : query.date();
        String grade = normalize(query.grade());
        String classId = normalize(query.classId());
        String keyword = query.keyword() == null || query.keyword().isBlank() ? null : query.keyword().trim();
        boolean abnormalOnly = query.abnormalOnly();
        List<String> classIds = AuthContext.scopedClassIds();

        AttendanceStatRow summaryRow = attendanceMapper.selectDaySummary(date, grade, classId, classIds);
        int present = ValueAssembler.value(summaryRow == null ? null : summaryRow.getPresent());
        int late = ValueAssembler.value(summaryRow == null ? null : summaryRow.getLate());
        int absent = ValueAssembler.value(summaryRow == null ? null : summaryRow.getAbsent());
        int leave = ValueAssembler.value(summaryRow == null ? null : summaryRow.getLeave());
        int total = present + late + absent + leave;
        double rate = total == 0 ? 0d : ValueAssembler.round4((double) (present + late) / total);

        AttendanceVos.Summary summary = new AttendanceVos.Summary(
                date,
                SchoolCalendar.isSchoolDay(date),
                total,
                new AttendanceVos.Counts(present, late, absent, leave),
                rate,
                (int) attendanceMapper.countMarked(date));

        Page<AttendanceRow> pageResult = PageHelper.startPage(safePage, safeSize)
                .doSelectPage(() -> attendanceMapper.selectDayPage(date, grade, classId, keyword,
                        abnormalOnly, classIds));
        List<AttendanceRow> rows = pageResult;
        List<AttendanceVos.Row> items = rows.stream()
                .map(row -> new AttendanceVos.Row(
                        row.getId(),
                        row.getSid(),
                        row.getName(),
                        row.getGender(),
                        row.getGrade(),
                        row.getClassId(),
                        row.getClassName(),
                        row.getDayStatus(),
                        assembler.toAttendanceStat(row.getPresent(), row.getLate(), row.getAbsent(),
                                row.getLeave())))
                .collect(Collectors.toList());

        return new AttendanceVos.Page(summary, PageResult.of(pageResult, items),
                SchoolCalendar.lastSchoolDays(date, 5));
    }

    /** 校验权限和考勤状态后写入单条考勤记录。 */
    @Override
    @Transactional
    public void mark(AttendanceDtos.MarkRequest request) {
        validateStatus(request.status());
        ensureStudentsInScope(List.of(request.studentId()));
        attendanceMapper.upsert(request.studentId(), request.date(), request.status(), request.remark());
    }

    /** 校验权限和考勤状态后批量写入考勤记录。 */
    @Override
    @Transactional
    public int markBatch(AttendanceDtos.BatchRequest request) {
        request.records().forEach(item -> validateStatus(item.status()));
        ensureStudentsInScope(request.records().stream().map(AttendanceDtos.BatchRequest.Item::studentId).toList());
        return attendanceMapper.batchUpsert(request.date(), request.records());
    }

    /** 写入前确认所有学生都属于当前班主任负责的班级。 */
    private void ensureStudentsInScope(List<Long> studentIds) {
        List<String> classIds = AuthContext.scopedClassIds();
        if (classIds == null) {
            return;
        }
        if (classIds.isEmpty()) {
            throw new BizException(ErrorCode.FORBIDDEN, "当前账号还没有分配班级，请联系系统管理员");
        }
        List<Long> outside = studentMapper.selectIdsOutsideScope(studentIds, classIds);
        if (outside != null && !outside.isEmpty()) {
            throw new BizException(ErrorCode.FORBIDDEN, "只能标记自己负责班级的学生考勤");
        }
    }

    /** 确认考勤状态属于系统允许的状态值。 */
    private void validateStatus(String status) {
        if (!Constants.ATTENDANCE_STATUSES.contains(status)) {
            throw new BizException(ErrorCode.PARAM_INVALID,
                    "考勤状态只能是：" + String.join("、", Constants.ATTENDANCE_STATUSES));
        }
    }

    /** 将空筛选项及“全部”统一转换为 null。 */
    private static String normalize(String value) {
        return value == null || value.isBlank() || "全部".equals(value) ? null : value.trim();
    }
}
