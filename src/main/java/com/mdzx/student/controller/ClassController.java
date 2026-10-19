package com.mdzx.student.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mdzx.student.common.ApiResponse;
import com.mdzx.student.service.ClassService;
import com.mdzx.student.vo.ClassVos;

/**
 * ClassController。
 * <p>接收并校验 HTTP 请求，调用业务服务并返回统一接口数据。</p>
 */
@RestController
@RequestMapping("/api/classes")
public class ClassController {

    private final ClassService classService;
    public ClassController(ClassService classService) {
        this.classService = classService;
    }

    /** 班级列表：人数、平均总分、出勤率、第一名 */
    @GetMapping
    public ApiResponse<List<ClassVos.Summary>> list(
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String sort) {
        return ApiResponse.ok(classService.list(grade, sort));
    }
}
