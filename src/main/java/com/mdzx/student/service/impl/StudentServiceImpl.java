package com.mdzx.student.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mdzx.student.common.BizException;
import com.mdzx.student.common.Constants;
import com.mdzx.student.common.ErrorCode;
import com.mdzx.student.common.PageResult;
import com.mdzx.student.security.AuthContext;
import com.mdzx.student.domain.AttendanceRecord;
import com.mdzx.student.domain.ClassInfo;
import com.mdzx.student.domain.Student;
import com.mdzx.student.domain.StudentScore;
import com.mdzx.student.domain.Subject;
import com.mdzx.student.dto.StudentDtos;
import com.mdzx.student.mapper.AttendanceMapper;
import com.mdzx.student.mapper.ClassMapper;
import com.mdzx.student.mapper.ScoreMapper;
import com.mdzx.student.mapper.StudentMapper;
import com.mdzx.student.mapper.SubjectMapper;
import com.mdzx.student.mapper.model.AttendanceStatRow;
import com.mdzx.student.mapper.model.GradeAggRow;
import com.mdzx.student.mapper.model.StudentRow;
import com.mdzx.student.service.StudentService;
import com.mdzx.student.service.support.ValueAssembler;
import com.mdzx.student.vo.StudentVos;

/**
 * StudentService。
 * <p>实现对应业务流程，组合数据访问、校验与权限规则。</p>
 */
@Service
public class StudentServiceImpl implements StudentService {

    private static final int MAX_PAGE_SIZE = 100;

    private final StudentMapper studentMapper;
    private final ScoreMapper scoreMapper;
    private final AttendanceMapper attendanceMapper;
    private final ClassMapper classMapper;
    private final SubjectMapper subjectMapper;
    private final ValueAssembler assembler;
    public StudentServiceImpl(StudentMapper studentMapper, ScoreMapper scoreMapper,
            AttendanceMapper attendanceMapper, ClassMapper classMapper, SubjectMapper subjectMapper,
            ValueAssembler assembler) {
        this.studentMapper = studentMapper;
        this.scoreMapper = scoreMapper;
        this.attendanceMapper = attendanceMapper;
        this.classMapper = classMapper;
        this.subjectMapper = subjectMapper;
        this.assembler = assembler;
    }

    /** 按筛选条件分页查询学生，并批量补齐成绩后组装成列表项。 */
    @Override
    public PageResult<StudentVos.Item> page(StudentDtos.Query query, int page, int size) {
        // 规范分页边界，避免非法页码及超大单页查询。
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        // 清理筛选值，并将查询对象中的选项转换为 SQL 所需条件。
        String keyword = normalize(query.keyword());
        String grade = normalize(query.grade());
        String classId = normalize(query.classId());
        String status = normalize(query.status());
        boolean absentAlert = query.abnormalAbsence();
        boolean incomplete = Boolean.TRUE.equals(query.incomplete());
        String sort = query.sort() == null ? null : query.sort();
        boolean desc = query.desc();
        // 获取当前登录人的班级数据范围，作为查询条件传入 Mapper。
        List<String> classIds = AuthContext.scopedClassIds();

        // 分页查询学生档案及成绩、考勤等列表字段。
        Page<StudentRow> pageResult = PageHelper.startPage(safePage, safeSize)
                .doSelectPage(() -> studentMapper.selectPage(keyword, grade, classId, status, absentAlert,
                        incomplete, sort, desc, null, classIds));
        List<StudentRow> rows = pageResult;
        // 一次批量读取本页学生的成绩，避免逐个学生查询。
        Map<Long, Map<String, BigDecimal>> scores = loadScores(rows);
        // 将数据库行和成绩映射成 API 列表项，再组装分页元数据。
        List<StudentVos.Item> items = rows.stream()
                .map(row -> assembler.toItem(row, scores.getOrDefault(row.getId(), Map.of())))
                .collect(Collectors.toList());
        return PageResult.of(pageResult, items);
    }

    /** 查询学生档案，同时组装成绩、近期考勤和年级均分。 */
    @Override
    public StudentVos.Detail detail(Long id) {
        // 查询档案行，不存在时立即返回业务错误。
        StudentRow row = studentMapper.selectRowById(id);
        if (row == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "学生不存在：" + id);
        }
        // 校验当前用户是否有权查看该学生所在班级。
        requireClassAccess(row.getClassId());
        // 查询学生各科成绩，并按学科编码整理成有序映射。
        Map<String, BigDecimal> scores = scoreMapper.selectByStudentId(id).stream()
                .collect(Collectors.toMap(StudentScore::getSubjectKey, StudentScore::getScore, (a, b) -> a,
                        LinkedHashMap::new));
        // 读取考勤统计和最近九条考勤记录。
        AttendanceStatRow stat = attendanceMapper.selectStatsByStudentId(id);
        List<AttendanceRecord> recent = attendanceMapper.selectRecentRecords(id, 9);
        // 将考勤记录按时间正序转换为详情出参。
        List<StudentVos.AttendanceDay> recentDays = new ArrayList<>(recent.size());
        for (int i = recent.size() - 1; i >= 0; i--) {
            AttendanceRecord record = recent.get(i);
            recentDays.add(new StudentVos.AttendanceDay(record.getAttendDate(), record.getStatus()));
        }
        // 汇总档案、成绩、年级均分、考勤和学科信息，构造详情响应。
        return new StudentVos.Detail(
                assembler.toItem(row, scores),
                gradeAverage(row.getGrade()),
                recentDays,
                assembler.toSubjects(subjectMapper.selectAll()));
    }

    /** 先校验班级、学号和成绩，再在同一事务中保存档案及成绩。 */
    @Override
    @Transactional
    public StudentVos.Item create(StudentDtos.SaveRequest request) {
        // 根据请求中的年级和班号解析目标班级。
        ClassInfo classInfo = resolveClass(request.grade(), request.classNo());
        // 确认当前用户有权在目标班级新增学生。
        requireClassAccess(classInfo.getClassId());
        // 校验学号唯一性，冲突时终止事务并返回业务错误。
        if (studentMapper.countBySid(request.sid(), null) > 0) {
            throw new BizException(ErrorCode.SID_DUPLICATED, "学号 " + request.sid() + " 已存在");
        }
        // 检查成绩学科和分数范围。
        validateScores(request.scores());

        // 将请求字段转换为实体并插入学生档案，数据库生成学生 ID。
        Student student = toEntity(null, request, classInfo.getClassId());
        studentMapper.insert(student);
        // 在同一事务中写入成绩，再读取组装好的学生列表项返回。
        saveScores(student.getId(), request.scores());
        return detail(student.getId()).student();
    }

    /** 更新学生数据**/
    @Override
    @Transactional
    public StudentVos.Item update(Long id, StudentDtos.SaveRequest request) {
        // 读取待更新学生，确认记录存在。
        Student existing = studentMapper.selectById(id);
        if (existing == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "学生不存在：" + id);
        }
        // 解析目标班级并检查当前用户对该班级的访问权限。
        ClassInfo classInfo = resolveClass(request.grade(), request.classNo());
        requireClassAccess(classInfo.getClassId());
        // 检查学号唯一性时排除当前学生本身。
        if (studentMapper.countBySid(request.sid(), id) > 0) {
            throw new BizException(ErrorCode.SID_DUPLICATED, "学号 " + request.sid() + " 已被占用");
        }
        // 校验请求中携带的成绩数据。
        validateScores(request.scores());

        // 转换请求并更新学生档案。
        Student student = toEntity(id, request, classInfo.getClassId());
        studentMapper.update(student);
        // 同事务保存成绩，再读取最新列表项作为响应。
        saveScores(id, request.scores());
        return detail(id).student();
    }

    /** 删除学生数据**/
    @Override
    @Transactional
    public void delete(Long id) {
        // 删除前确认学生存在。
        if (studentMapper.selectById(id) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "学生不存在：" + id);
        }
        // 删除学生档案；student_score 和 attendance_record 由外键级联删除。
        studentMapper.deleteById(id);
    }

    /** 根据id批量删除学生数据**/
    @Override
    @Transactional
    public int deleteBatch(List<Long> ids) {
        // 拒绝空编号集合，避免无效批量删除请求。
        if (ids == null || ids.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "请先选择要删除的学生");
        }
        // 执行批量删除并返回实际影响行数。
        return studentMapper.deleteByIds(ids);
    }

    /** 读取表单所需的下拉选项。 */
    @Override
    public StudentVos.Options options() {
        // 查询当前用户可见班级并转换为表单选项。
        List<StudentVos.ClassOption> classes = classMapper.selectAll(null, AuthContext.scopedClassIds()).stream()
                .map(c -> new StudentVos.ClassOption(c.getClassId(), c.getClassName(), c.getGrade(),
                        c.getGradeIndex(), c.getClassNo(), c.getHeadTeacher(), c.getCapacity()))
                .collect(Collectors.toList());
        // 组合年级、班级、状态和学科选项并返回。
        return new StudentVos.Options(Constants.GRADES, classes, Constants.STATUSES,
                assembler.toSubjects(subjectMapper.selectAll()));
    }

    // ---------------- 内部方法 ----------------

    /** 将一批学生的成绩按学生 id 和学科编码分组，避免逐个学生查询。 */
    private Map<Long, Map<String, BigDecimal>> loadScores(Collection<StudentRow> rows) {
        // 空列表无需访问成绩表。
        if (rows.isEmpty()) {
            return Map.of();
        }
        // 提取学生 ID 并一次查询本页涉及的全部成绩。
        List<Long> ids = rows.stream().map(StudentRow::getId).collect(Collectors.toList());
        List<StudentScore> scores = scoreMapper.selectByStudentIds(ids);
        // 按学生 ID 和学科编码分组，供列表组装时快速读取。
        Map<Long, Map<String, BigDecimal>> grouped = new HashMap<>();
        for (StudentScore score : scores) {
            grouped.computeIfAbsent(score.getStudentId(), key -> new LinkedHashMap<>())
                    .put(score.getSubjectKey(), score.getScore());
        }
        return grouped;
    }

    /** 计算指定年级在当前数据范围内的平均总分。 */
    private double gradeAverage(String grade) {
        // 查询当前数据范围内的年级聚合，选出目标年级均分并保留一位小数。
        return studentMapper.selectGradeAggregates(AuthContext.scopedClassIds()).stream()
                .filter(row -> grade.equals(row.getGrade()))
                .map(GradeAggRow::getAvgTotal)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .map(value -> BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue())
                .findFirst()
                .orElse(0d);
    }

    /** 根据年级和班号解析班级，同时应用当前用户可访问的班级范围。 */
    private ClassInfo resolveClass(String grade, Integer classNo) {
        // 缺少班号时直接视为请求参数错误。
        if (classNo == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "请选择班级");
        }
        // 在当前用户可见班级内匹配年级和班号，不存在时返回参数错误。
        return classMapper.selectAll(grade, AuthContext.scopedClassIds()).stream()
                .filter(info -> classNo.equals(info.getClassNo()))
                .findFirst()
                .orElseThrow(() -> new BizException(ErrorCode.PARAM_INVALID,
                        "班级不存在：" + grade + "（" + classNo + "）班"));
    }

    /** 检查成绩学科是否存在，并确认分数落在该学科满分范围内。 */
    private void validateScores(Map<String, BigDecimal> scores) {
        // 没有提交成绩时无需校验。
        if (scores == null || scores.isEmpty()) {
            return;
        }
        // 读取学科满分配置，逐项校验学科是否存在及分数范围。
        Map<String, Integer> fullScores = subjectMapper.selectAll().stream()
                .collect(Collectors.toMap(Subject::getSubjectKey, Subject::getFullScore));
        scores.forEach((key, value) -> {
            Integer full = fullScores.get(key);
            if (full == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "未知学科：" + key);
            }
            if (value == null) {
                return;
            }
            if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.valueOf(full)) > 0) {
                throw new BizException(ErrorCode.PARAM_INVALID,
                        "「" + key + "」成绩需在 0–" + full + " 之间");
            }
        });
    }

    /** 批量保存学生成绩；已有学科成绩会按主键更新。 */
    private void saveScores(Long studentId, Map<String, BigDecimal> scores) {
        // 未提交成绩时不执行写入。
        if (scores == null || scores.isEmpty()) {
            return;
        }
        // 忽略未填写科目的空值，并转换为成绩实体列表。
        List<StudentScore> list = scores.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> new StudentScore(studentId, entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        // 有有效成绩时批量新增或更新。
        if (!list.isEmpty()) {
            scoreMapper.batchUpsert(list);
        }
    }

    /** 将请求字段转换为学生实体，并应用默认值和空白值处理。 */
    private Student toEntity(Long id, StudentDtos.SaveRequest request, String classId) {
        // 创建实体并设置主键、学籍字段及已解析的班级 ID。
        Student student = new Student();
        student.setId(id);
        student.setSid(request.sid());
        student.setName(request.name().trim());
        student.setGender(request.gender());
        student.setClassId(classId);
        student.setStatus(request.status());
        // 清理可空文本，并为缺省生日和住宿方式应用默认值。
        student.setPhone(blankToNull(request.phone()));
        student.setGuardian(request.guardian() == null ? null : request.guardian().trim());
        student.setGuardianRelation(request.guardianRelation());
        student.setGuardianPhone(blankToNull(request.guardianPhone()));
        student.setBirthday(request.birthday() == null ? LocalDate.of(2010, 1, 1) : request.birthday());
        student.setEnrollmentDate(request.enrollmentDate());
        student.setAddress(request.address());
        student.setDorm(request.dorm() == null || request.dorm().isBlank() ? "走读" : request.dorm());
        student.setNote(request.note());
        return student;
    }

    /** 将空筛选项及“全部”统一转换为 null。 */
    private static String normalize(String value) {
        // 空值、空白和“全部”都代表不添加该筛选条件。
        if (value == null || value.isBlank() || "全部".equals(value)) {
            return null;
        }
        return value.trim();
    }

    /** 班主任只能看/改自己负责班级的学生 */
    private void requireClassAccess(String classId) {
        // 当前用户无法访问该班级时拒绝继续读取或写入学生数据。
        if (!AuthContext.canAccessClass(classId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "该学生不在你负责的班级内");
        }
    }

    /** 将空白字符串转换为 null，并去除非空值两端空格。 */
    private static String blankToNull(String value) {
        // 将空白文本转换成 null，其余文本去除首尾空格。
        return value == null || value.isBlank() ? null : value.trim();
    }
}
