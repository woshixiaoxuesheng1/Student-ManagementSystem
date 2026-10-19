package com.mdzx.student.domain;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Subject。
 * <p>表示系统中的持久化业务实体及其字段。</p>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class Subject {

    private String subjectKey;
    private String subjectName;
    private Integer fullScore;
    private Integer sortNo;
}
