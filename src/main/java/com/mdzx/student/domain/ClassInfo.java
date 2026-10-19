package com.mdzx.student.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;


/**
 * ClassInfo。
 * <p>表示系统中的持久化业务实体及其字段。</p>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class ClassInfo {

    private String classId;
    private String className;
    private String grade;
    private Integer gradeIndex;
    private Integer classNo;
    private String headTeacher;
    private Integer capacity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
