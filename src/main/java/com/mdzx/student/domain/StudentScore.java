package com.mdzx.student.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * StudentScore。
 * <p>表示系统中的持久化业务实体及其字段。</p>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class StudentScore {

    private Long studentId;
    private String subjectKey;
    private BigDecimal score;
    private LocalDateTime updatedAt;

    /** 创建包含学生编号、学科编码和分数的成绩实体。 */
    public StudentScore(Long studentId, String subjectKey, BigDecimal score) {
        this.studentId = studentId;
        this.subjectKey = subjectKey;
        this.score = score;
    }
}
