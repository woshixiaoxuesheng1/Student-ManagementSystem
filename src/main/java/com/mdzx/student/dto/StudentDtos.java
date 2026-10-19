package com.mdzx.student.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 学生模块的入参 */
public final class StudentDtos {

    /** 工具类仅提供静态数据或方法，不需要创建实例。 */
    StudentDtos() {
    }

    /** 列表查询条件（与前端 URL 查询参数一一对应） */
    public record Query(
            String keyword,
            String grade,
            String classId,
            String status,
            String flag,
            Boolean incomplete,
            String sort,
            String dir
    ) {
    /** 判断是否启用缺勤异常筛选。 */
    public boolean abnormalAbsence() {
            return "缺勤偏多".equals(flag);
    }

    /** 根据排序方向参数判断是否采用降序。 */
    public boolean desc() {
            return !"asc".equalsIgnoreCase(dir);
    }
    }

    /** 新增或编辑学生档案时提交的数据及字段校验规则。 */
    public record SaveRequest(
            @NotBlank(message = "请填写学生姓名") @Size(min = 2, max = 32, message = "姓名长度需在 2–32 之间") String name,
            @NotBlank(message = "请选择性别") @Pattern(regexp = "男|女", message = "性别只能是男或女") String gender,
            @NotBlank(message = "请填写学号") @Pattern(regexp = "\\d{8}", message = "学号为 8 位数字") String sid,
            @NotBlank(message = "请选择年级") String grade,
            @Min(value = 1, message = "班号至少为 1") @Max(value = 20, message = "班号过大") Integer classNo,
            @NotBlank(message = "请选择学籍状态") String status,
            LocalDate birthday,
            LocalDate enrollmentDate,
            @Pattern(regexp = "^$|1[3-9]\\d{9}", message = "学生手机号格式不正确") String phone,
            @NotBlank(message = "请填写监护人姓名") String guardian,
            String guardianRelation,
            @Pattern(regexp = "^$|1[3-9]\\d{9}", message = "监护人手机号格式不正确") String guardianPhone,
            String address,
            String dorm,
            @Size(max = 255, message = "异动说明不超过 255 字") String note,
            Map<String, BigDecimal> scores
    ) {
    }
}
