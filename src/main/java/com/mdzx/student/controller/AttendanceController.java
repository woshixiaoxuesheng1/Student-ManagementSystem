package com.mdzx.student.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mdzx.student.common.ApiResponse;
import com.mdzx.student.dto.AttendanceDtos;
import com.mdzx.student.service.AttendanceService;
import com.mdzx.student.vo.AttendanceVos;

import jakarta.validation.Valid;

/** 考勤接口：提供按日查询、单人标记和批量标记。 */
@RestController
@RequestMapping("/api/attendance")//映射请求地址的注解 表示这个Controller下的所有请求都在/api/attandance下 放在方法上
// 如果放在方法上表示这个方法处理方法
public class AttendanceController {

    private final AttendanceService attendanceService;

    // 构造器注入 可以省略@Autowired
    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    /** 某天的考勤：汇总 + 逐人状态 + 本学期累计 */
    @GetMapping
    public ApiResponse<AttendanceVos.Page> page(
            // @RequestParam 接受前端发送的请求参数 括号内表示该参数不是必须传的
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ApiResponse.ok(
                attendanceService.page(new AttendanceDtos.Query(date, scope, grade, classId, keyword), page, size));
    }

    /** 单人标记：同一学生同一天重复标记会被覆盖 */
    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<Void> mark(@Valid @RequestBody AttendanceDtos.MarkRequest request) {
        attendanceService.mark(request);
        return ApiResponse.ok();
    }

    /** 批量标记（例如整班一次提交） */
    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<Integer> markBatch(@Valid @RequestBody AttendanceDtos.BatchRequest request) {
        return ApiResponse.ok(attendanceService.markBatch(request));
    }
}
