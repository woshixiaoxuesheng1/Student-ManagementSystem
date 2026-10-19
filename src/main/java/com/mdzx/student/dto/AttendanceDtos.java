package com.mdzx.student.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** dto: Data Transfer Object 数据传输对象
 * 重点：接收客户端传过来的数据，或在不同层之间传输数据。
 * 例如新增学生时，前端提交姓名、性别、班级 ID，后端使用 StudentDtos.SaveRequest 接收请求，并可以通过 @Valid 校验参数。**/
/** 考勤模块的入参 */
public final class AttendanceDtos {

    /** 工具类仅提供静态数据或方法，不需要创建实例。 */
    private AttendanceDtos() {
    }

    /** 列表查询条件（与前端 URL 查询参数一一对应） */
    public record Query(LocalDate date, String scope, String grade, String classId, String keyword) {
        /** 判断查询条件是否只包含异常考勤。 */
        public boolean abnormalOnly() {
            return "abnormal".equalsIgnoreCase(scope);
        }
    }

    /** 单人标记 */
    public record MarkRequest(
            @NotNull(message = "缺少学生 id") Long studentId,
            @NotNull(message = "缺少考勤日期") LocalDate date,
            @NotBlank(message = "缺少考勤状态") String status,
            String remark) {
    }

    /** 批量标记 */
    public record BatchRequest(
            @NotNull(message = "缺少考勤日期") LocalDate date,
            @NotEmpty(message = "请至少提交一条考勤") @Valid List<Item> records) {
        /** 批量登记中的单名学生考勤数据。 */
        public record Item(
                @NotNull(message = "缺少学生 id") Long studentId,
                @NotBlank(message = "缺少考勤状态") String status) {
        }
    }
}
