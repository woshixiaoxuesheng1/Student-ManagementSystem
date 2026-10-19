package com.mdzx.student.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>domain ：一般存的业务实体</p>
 * <p>例如学生、班级、教师等对象，可能包含学生 ID、姓名、性别、班级 ID 等属性。具体项目中也可能使用 entity、model 等名称来承担类似职责</p>
 * AttendanceRecord。
 * <p>表示系统中的持久化业务实体及其字段。</p>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class AttendanceRecord {

    private Long id;
    private Long studentId;
    private LocalDate attendDate;
    private String status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
