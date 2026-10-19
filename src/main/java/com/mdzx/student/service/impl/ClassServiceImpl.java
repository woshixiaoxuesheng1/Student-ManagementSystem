package com.mdzx.student.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.mdzx.student.domain.ClassInfo;
import com.mdzx.student.mapper.ClassMapper;
import com.mdzx.student.mapper.model.ClassAggRow;
import com.mdzx.student.mapper.model.StudentBriefRow;
import com.mdzx.student.security.AuthContext;
import com.mdzx.student.service.ClassService;
import com.mdzx.student.service.support.ValueAssembler;
import com.mdzx.student.vo.ClassVos;

/**
 * ClassService。
 * <p>实现对应业务流程，组合数据访问、校验与权限规则。</p>
 */
@Service
public class ClassServiceImpl implements ClassService {

    private final ClassMapper classMapper;
    public ClassServiceImpl(ClassMapper classMapper) {
        this.classMapper = classMapper;
    }

    /** 按条件查询列表并组装返回数据。 */
    @Override
    public List<ClassVos.Summary> list(String grade, String sort) {
        String normalizedGrade = grade == null || grade.isBlank() || "全部".equals(grade) ? null : grade.trim();
        List<String> classIds = AuthContext.scopedClassIds();
        List<ClassInfo> classes = classMapper.selectAll(normalizedGrade, classIds);
        Map<String, ClassAggRow> aggregates = classMapper.selectAggregates(classIds).stream()
                .collect(Collectors.toMap(ClassAggRow::getClassId, Function.identity(), (a, b) -> a));
        Map<String, StudentBriefRow> topStudents = classMapper.selectTopStudents(classIds).stream()
                .collect(Collectors.toMap(StudentBriefRow::getClassId, Function.identity(), (a, b) -> a));

        List<ClassVos.Summary> summaries = classes.stream().map(info -> {
            ClassAggRow agg = aggregates.get(info.getClassId());
            StudentBriefRow top = topStudents.get(info.getClassId());
            return new ClassVos.Summary(
                    info.getClassId(),
                    info.getClassName(),
                    info.getGrade(),
                    info.getGradeIndex(),
                    info.getClassNo(),
                    info.getHeadTeacher(),
                    info.getCapacity(),
                    agg == null ? 0 : ValueAssembler.value(agg.getTotalCount()),
                    agg == null ? 0 : ValueAssembler.value(agg.getActiveCount()),
                    agg == null || agg.getAvgTotal() == null
                            ? BigDecimal.ZERO
                            : agg.getAvgTotal().setScale(1, RoundingMode.HALF_UP),
                    agg == null || agg.getAvgRate() == null ? 0d : ValueAssembler.round4(agg.getAvgRate()),
                    top == null ? null : top.getName(),
                    top == null ? null : top.getTotal());
        }).collect(Collectors.toList());

        Comparator<ClassVos.Summary> comparator = switch (sort == null ? "" : sort) {
            case "avg" -> Comparator.comparing(ClassVos.Summary::avgTotal).reversed();
            case "count" -> Comparator.comparingInt(ClassVos.Summary::count).reversed();
            case "rate" -> Comparator.comparingDouble(ClassVos.Summary::attendanceRate).reversed();
            default -> Comparator.comparingInt(ClassVos.Summary::gradeIndex)
                    .thenComparingInt(ClassVos.Summary::classNo);
        };
        summaries.sort(comparator);
        return summaries;
    }
}
