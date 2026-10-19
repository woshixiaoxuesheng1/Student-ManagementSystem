package com.mdzx.student.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mdzx.student.common.ApiResponse;
import com.mdzx.student.dto.GradeDtos;
import com.mdzx.student.service.GradeService;
import com.mdzx.student.service.support.ValueAssembler;
import com.mdzx.student.mapper.SubjectMapper;
import com.mdzx.student.vo.GradeVos;
import com.mdzx.student.vo.StudentVos;

/**
 * GradeController。
 * <p>接收并校验 HTTP 请求，调用业务服务并返回统一接口数据。</p>
 */
@RestController
@RequestMapping("/api/grades")
public class GradeController {

    private final GradeService gradeService;
    private final SubjectMapper subjectMapper;
    private final ValueAssembler assembler;
    public GradeController(GradeService gradeService, SubjectMapper subjectMapper, ValueAssembler assembler) {
        this.gradeService = gradeService;
        this.subjectMapper = subjectMapper;
        this.assembler = assembler;
    }

    /** 成绩列表 + 统计：sort 支持 total 或学科编码（chinese/math/...） */
    @GetMapping
    public ApiResponse<GradeVos.Page> page(
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String dir,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(gradeService.page(new GradeDtos.Query(grade, classId, keyword, sort, dir), page, size));
    }

    /** 学科配置：编码、名称、满分、顺序 */
    @GetMapping("/subjects")
    public ApiResponse<List<StudentVos.Subject>> subjects() {
        return ApiResponse.ok(assembler.toSubjects(subjectMapper.selectAll()));
    }
}
