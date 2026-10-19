package com.mdzx.student.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.mdzx.student.common.Constants;
import com.mdzx.student.common.SchoolCalendar;
import com.mdzx.student.domain.ClassInfo;
import com.mdzx.student.domain.SchoolSetting;
import com.mdzx.student.mapper.AttendanceMapper;
import com.mdzx.student.mapper.ClassMapper;
import com.mdzx.student.mapper.SettingMapper;
import com.mdzx.student.mapper.StudentMapper;
import com.mdzx.student.mapper.SubjectMapper;
import com.mdzx.student.mapper.model.AttendanceStatRow;
import com.mdzx.student.mapper.model.GradeAggRow;
import com.mdzx.student.mapper.model.GradeStatsRow;
import com.mdzx.student.mapper.model.StudentRow;
import com.mdzx.student.mapper.model.TrendRow;
import com.mdzx.student.security.AuthContext;
import com.mdzx.student.service.DashboardService;
import com.mdzx.student.service.support.ValueAssembler;
import com.mdzx.student.vo.DashboardVos;

/**
 * DashboardService。
 * <p>实现对应业务流程，组合数据访问、校验与权限规则。</p>
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    private static final int TREND_WEEKS = 8;

    private final StudentMapper studentMapper;
    private final ClassMapper classMapper;
    private final AttendanceMapper attendanceMapper;
    private final SubjectMapper subjectMapper;
    private final SettingMapper settingMapper;
    private final ValueAssembler assembler;
    public DashboardServiceImpl(StudentMapper studentMapper, ClassMapper classMapper,
            AttendanceMapper attendanceMapper, SubjectMapper subjectMapper, SettingMapper settingMapper,
            ValueAssembler assembler) {
        this.studentMapper = studentMapper;
        this.classMapper = classMapper;
        this.attendanceMapper = attendanceMapper;
        this.subjectMapper = subjectMapper;
        this.settingMapper = settingMapper;
        this.assembler = assembler;
    }

    /** 以最近教学日为统计口径，汇总首页指标、趋势、待办和年级数据。 */
    @Override
    public DashboardVos.Overview overview(LocalDate date) {
        LocalDate today = date == null ? LocalDate.now() : date;
        LocalDate schoolDay = SchoolCalendar.latestSchoolDay(today);
        List<String> classIds = AuthContext.scopedClassIds();

        long totalStudents = studentMapper.countAll(classIds);
        long activeStudents = studentMapper.countByStatus(Constants.STATUS_ACTIVE, classIds);
        long suspendedStudents = studentMapper.countByStatus("休学", classIds);
        List<ClassInfo> classes = classMapper.selectAll(null, classIds);
        int fullScore = subjectMapper.selectAll().stream().mapToInt(subject -> subject.getFullScore()).sum();

        AttendanceStatRow dayStat = attendanceMapper.selectDaySummary(schoolDay, null, null, classIds);
        int present = ValueAssembler.value(dayStat == null ? null : dayStat.getPresent());
        int late = ValueAssembler.value(dayStat == null ? null : dayStat.getLate());
        int absent = ValueAssembler.value(dayStat == null ? null : dayStat.getAbsent());
        int leave = ValueAssembler.value(dayStat == null ? null : dayStat.getLeave());
        int dayTotal = present + late + absent + leave;
        double dayRate = dayTotal == 0 ? 0d : (double) (present + late) / dayTotal;

        GradeStatsRow activeStats = studentMapper.selectActiveStats(classIds);

        List<DashboardVos.TrendPoint> trend = buildTrend(schoolDay, classIds);
        double delta = trend.size() < 2
                ? 0d
                : ValueAssembler.round4(trend.get(trend.size() - 1).rate() - trend.get(trend.size() - 2).rate());

        DashboardVos.Metrics metrics = new DashboardVos.Metrics(
                totalStudents,
                activeStudents,
                suspendedStudents,
                classes.size(),
                classes.isEmpty() ? 0d : ValueAssembler.round4((double) totalStudents / classes.size()),
                schoolDay,
                ValueAssembler.round4(dayRate),
                new DashboardVos.AttendanceCounts(present, late, absent, leave),
                activeStats == null || activeStats.getAvgTotal() == null
                        ? BigDecimal.ZERO
                        : activeStats.getAvgTotal().setScale(1, RoundingMode.HALF_UP),
                activeStats == null || activeStats.getBestTotal() == null
                        ? BigDecimal.ZERO
                        : activeStats.getBestTotal(),
                fullScore,
                delta);

        SchoolSetting setting = settingMapper.select();
        return new DashboardVos.Overview(
                setting == null ? "" : setting.getTerm(),
                setting == null ? "" : setting.getSchoolName(),
                today,
                metrics,
                trend,
                buildTodos(classIds),
                buildGradeSummaries(classIds),
                buildRecentMoves(classIds));
    }

    /** 将数据库按周统计结果补齐为连续趋势点，便于前端直接绘图。 */
    private List<DashboardVos.TrendPoint> buildTrend(LocalDate endDate, List<String> classIds) {
        List<TrendRow> rows = attendanceMapper.selectWeeklyTrend(endDate.minusWeeks(TREND_WEEKS), endDate, null, null,
                classIds);
        int from = Math.max(0, rows.size() - TREND_WEEKS);
        List<TrendRow> window = rows.subList(from, rows.size());
        List<DashboardVos.TrendPoint> points = new ArrayList<>(window.size());
        for (int i = 0; i < window.size(); i++) {
            TrendRow row = window.get(i);
            points.add(new DashboardVos.TrendPoint(i + 1, "第 " + (i + 1) + " 周", row.getStartDate(),
                    ValueAssembler.round4(row.getRate() == null ? 0d : row.getRate())));
        }
        return points;
    }

    /** 根据当前数据范围生成概览页待办事项。 */
    private List<DashboardVos.Todo> buildTodos(List<String> classIds) {
        List<DashboardVos.Todo> todos = new ArrayList<>();
        long suspended = studentMapper.countByStatus("休学", classIds);
        if (suspended > 0) {
            todos.add(new DashboardVos.Todo("todo-suspend", "休学学生到期复核", "核对休学期限与复学材料", suspended, "warn",
                    "查看名单", Map.of("status", "休学")));
        }
        long absentAlert = studentMapper.countAbsentAlert(Constants.ABSENT_ALERT_THRESHOLD, classIds);
        if (absentAlert > 0) {
            todos.add(new DashboardVos.Todo("todo-absent", "缺勤次数达 " + Constants.ABSENT_ALERT_THRESHOLD + " 次及以上",
                    "建议联系家长并记录沟通结果", absentAlert, "danger", "查看名单", Map.of("flag", "缺勤偏多")));
        }
        long missingContact = studentMapper.countMissingGuardianPhone(classIds);
        if (missingContact > 0) {
            todos.add(new DashboardVos.Todo("todo-contact", "监护人联系方式缺失", "学籍档案资料不完整", missingContact, "info",
                    "查看名单", Map.of("incomplete", "1")));
        }
        long transferring = studentMapper.countByStatus("转学", classIds)
                + studentMapper.countByStatus("退学", classIds);
        if (transferring > 0) {
            todos.add(new DashboardVos.Todo("todo-flow", "转学 / 退学待归档", "补充异动原因并归档纸质材料", transferring, "neutral",
                    "查看名单", Map.of("status", "转学")));
        }
        return todos;
    }

    /** 按年级汇总学生人数、成绩和考勤指标。 */
    private List<DashboardVos.GradeSummary> buildGradeSummaries(List<String> classIds) {
        return studentMapper.selectGradeAggregates(classIds).stream()
                .map(row -> new DashboardVos.GradeSummary(
                        row.getGrade(),
                        ValueAssembler.value(row.getClassCount()),
                        ValueAssembler.value(row.getTotalCount()),
                        ValueAssembler.value(row.getActiveCount()),
                        row.getAvgTotal() == null ? BigDecimal.ZERO
                                : row.getAvgTotal().setScale(1, RoundingMode.HALF_UP),
                        row.getAvgRate() == null ? 0d : ValueAssembler.round4(row.getAvgRate())))
                .collect(Collectors.toList());
    }

    /** 查询并组装近期学籍异动记录。 */
    private List<DashboardVos.Move> buildRecentMoves(List<String> classIds) {
        return studentMapper.selectRecentMoves(5, classIds).stream()
                .map(row -> new DashboardVos.Move(
                        row.getId(),
                        row.getSid(),
                        row.getName(),
                        row.getGrade(),
                        row.getClassName(),
                        row.getStatus(),
                        assembler.toAttendanceStat(row.getPresent(), row.getLate(), row.getAbsent(), row.getLeave())
                                .rate()))
                .collect(Collectors.toList());
    }

    /** 保留给后续扩展：年级维度的出勤率明细 */
    @SuppressWarnings("unused")
    private Map<String, Double> gradeRates() {
        Map<String, Double> rates = new LinkedHashMap<>();
        for (GradeAggRow row : studentMapper.selectGradeAggregates(null)) {
            rates.put(row.getGrade(), row.getAvgRate() == null ? 0d : row.getAvgRate());
        }
        return rates;
    }
}
