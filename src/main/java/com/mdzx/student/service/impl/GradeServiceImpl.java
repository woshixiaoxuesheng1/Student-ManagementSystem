package com.mdzx.student.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.stereotype.Service;

import com.mdzx.student.common.Constants;
import com.mdzx.student.common.PageResult;
import com.mdzx.student.domain.StudentScore;
import com.mdzx.student.domain.Subject;
import com.mdzx.student.dto.GradeDtos;
import com.mdzx.student.security.AuthContext;
import com.mdzx.student.mapper.ScoreMapper;
import com.mdzx.student.mapper.StudentMapper;
import com.mdzx.student.mapper.SubjectMapper;
import com.mdzx.student.mapper.model.GradeStatsRow;
import com.mdzx.student.mapper.model.StudentRow;
import com.mdzx.student.service.GradeService;
import com.mdzx.student.service.support.ValueAssembler;
import com.mdzx.student.vo.GradeVos;

/**
 * GradeService。
 * <p>实现对应业务流程，组合数据访问、校验与权限规则。</p>
 */
@Service
public class GradeServiceImpl implements GradeService {

    private static final int MAX_PAGE_SIZE = 100;

    private final StudentMapper studentMapper;
    private final ScoreMapper scoreMapper;
    private final SubjectMapper subjectMapper;
    private final ValueAssembler assembler;
    public GradeServiceImpl(StudentMapper studentMapper, ScoreMapper scoreMapper, SubjectMapper subjectMapper,
            ValueAssembler assembler) {
        this.studentMapper = studentMapper;
        this.scoreMapper = scoreMapper;
        this.subjectMapper = subjectMapper;
        this.assembler = assembler;
    }

    /** 根据查询参数返回分页结果及对应统计信息。 */
    @Override
    public GradeVos.Page page(GradeDtos.Query query, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String keyword = normalize(query.keyword());
        String grade = normalize(query.grade());
        String classId = normalize(query.classId());
        boolean desc = query.dir() == null || query.dir().isBlank() || query.desc();

        List<Subject> subjects = subjectMapper.selectAll();
        int fullScore = subjects.stream().mapToInt(Subject::getFullScore).sum();
        int passLine = (int) Math.round(fullScore * Constants.PASS_RATIO);
        int excellentLine = (int) Math.round(fullScore * Constants.EXCELLENT_RATIO);

        String sortKey;
        String subjectKey;
        if (query.sort() != null) {
            String requested = query.sort().trim();
            if (subjects.stream().anyMatch(subject -> subject.getSubjectKey().equals(requested))) {
                sortKey = "subject";
                subjectKey = requested;
            } else {
                subjectKey = null;
                sortKey = "total";
            }
        } else {
            subjectKey = null;
            sortKey = "total";
        }

        List<String> classIds = AuthContext.scopedClassIds();
        Page<StudentRow> pageResult = PageHelper.startPage(safePage, safeSize)
                .doSelectPage(() -> studentMapper.selectPage(keyword, grade, classId, Constants.STATUS_ACTIVE,
                        false, false, sortKey, desc, subjectKey, classIds));
        List<StudentRow> rows = pageResult;

        Map<Long, Map<String, BigDecimal>> scores = loadScores(rows);
        List<GradeVos.Row> items = rows.stream()
                .map(row -> new GradeVos.Row(
                        row.getId(),
                        row.getSid(),
                        row.getName(),
                        row.getGender(),
                        row.getGrade(),
                        row.getClassId(),
                        row.getClassName(),
                        row.getStatus(),
                        ValueAssembler.nullToZero(row.getTotal()),
                        row.getGradeRank(),
                        scores.getOrDefault(row.getId(), Map.of())))
                .collect(Collectors.toList());

        GradeStatsRow statsRow = studentMapper.selectStats(keyword, grade, classId, passLine, excellentLine, classIds);
        StudentRow best = studentMapper.selectTopByFilter(keyword, grade, classId, classIds);
        int studentCount = statsRow == null ? 0 : ValueAssembler.value(statsRow.getStudentCount());
        int passCount = statsRow == null ? 0 : ValueAssembler.value(statsRow.getPassCount());
        int excellentCount = statsRow == null ? 0 : ValueAssembler.value(statsRow.getExcellentCount());

        GradeVos.Stats stats = new GradeVos.Stats(
                studentCount,
                statsRow == null || statsRow.getAvgTotal() == null
                        ? BigDecimal.ZERO
                        : statsRow.getAvgTotal().setScale(1, RoundingMode.HALF_UP),
                best == null ? BigDecimal.ZERO : ValueAssembler.nullToZero(best.getTotal()),
                best == null ? null : best.getName(),
                best == null ? null : best.getClassName(),
                fullScore,
                passLine,
                excellentLine,
                studentCount == 0 ? 0d : ValueAssembler.round4((double) passCount / studentCount),
                studentCount == 0 ? 0d : ValueAssembler.round4((double) excellentCount / studentCount));

        return new GradeVos.Page(PageResult.of(pageResult, items), stats,
                assembler.toSubjects(subjects));
    }

    /** 批量读取成绩并按学生编号、学科编码分组。 */
    private Map<Long, Map<String, BigDecimal>> loadScores(List<StudentRow> rows) {
        if (rows.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = rows.stream().map(StudentRow::getId).collect(Collectors.toList());
        Map<Long, Map<String, BigDecimal>> grouped = new LinkedHashMap<>();
        for (StudentScore score : scoreMapper.selectByStudentIds(ids)) {
            grouped.computeIfAbsent(score.getStudentId(), key -> new LinkedHashMap<>())
                    .put(score.getSubjectKey(), score.getScore());
        }
        return grouped;
    }

    /** 将空筛选项及“全部”统一转换为 null。 */
    private static String normalize(String value) {
        return value == null || value.isBlank() || "全部".equals(value) ? null : value.trim();
    }
}
