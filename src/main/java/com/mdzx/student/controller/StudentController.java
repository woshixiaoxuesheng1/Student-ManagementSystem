package com.mdzx.student.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mdzx.student.common.ApiResponse;
import com.mdzx.student.common.PageResult;
import com.mdzx.student.dto.StudentDtos;
import com.mdzx.student.service.StudentService;
import com.mdzx.student.vo.StudentVos;

import jakarta.validation.Valid;

/**
 * StudentController。
 * <p>接收并校验 HTTP 请求，调用业务服务并返回统一接口数据。</p>
 */
@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;
    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    /** 学生列表：与前端 URL 查询参数同名 */
    @GetMapping
    public ApiResponse<PageResult<StudentVos.Item>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String flag,
            @RequestParam(required = false) Boolean incomplete,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String dir,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        // 将 URL 查询参数组合成业务层查询对象。
        StudentDtos.Query query = new StudentDtos.Query(keyword, grade, classId, status, flag, incomplete, sort, dir);
        // 调用分页查询服务，并包装成统一成功响应。
        return ApiResponse.ok(studentService.page(query, page, size));
    }

    /** 下拉框选项：年级、班级（含班主任）、学籍状态、学科 */
    @GetMapping("/options")
    public ApiResponse<StudentVos.Options> options() {
        // 查询学生表单需要的年级、班级、状态和学科选项。
        return ApiResponse.ok(studentService.options());
    }

    /** 按编号查询并返回记录详情。 */
    @GetMapping("/{id}")
    public ApiResponse<StudentVos.Detail> detail(@PathVariable Long id) {
        // 按学生编号读取档案及关联详情，并包装统一响应。
        return ApiResponse.ok(studentService.detail(id));
    }

    /** 新增学生信息
     * <p>@PreAuthorize 限制调用该功能的角色</p>**/
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")// StudentDtos.SaveRequest 调用类的内部类表示SaveRequest的数据类型
    public ApiResponse<StudentVos.Item> create(@Valid @RequestBody StudentDtos.SaveRequest request) {
        // Spring 先校验请求体字段，再交由业务层完成新增。
        return ApiResponse.ok(studentService.create(request));
    }

    /** 校验请求数据并更新指定记录。 */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<StudentVos.Item> update(@PathVariable Long id,
            @Valid @RequestBody StudentDtos.SaveRequest request) {
        // 传递学生编号和已校验的请求体，执行更新并返回最新记录。
        return ApiResponse.ok(studentService.update(id, request));
    }

    /** 删除指定记录。 */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        // 执行单条删除；关联清理由数据库外键级联处理。
        studentService.delete(id);
        // 删除成功时返回无数据的统一响应。
        return ApiResponse.ok();
    }

    /** 批量删除：body { "ids": [1, 2, 3] } */
    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Integer> deleteBatch(@RequestBody IdsPayload payload) {
        // 将请求体中的学生编号列表交给业务层批量删除。
        return ApiResponse.ok(studentService.deleteBatch(payload.ids()));
    }

    /** 批量删除学生时请求体中的学生编号集合。 */
    public record IdsPayload(List<Long> ids) {
    }
}
