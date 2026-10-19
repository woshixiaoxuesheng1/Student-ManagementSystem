package com.mdzx.student.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;


/**
 * Student。
 * <p>表示系统中的持久化业务实体及其字段。</p>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class Student {

    private Long id;
    private String sid;
    private String name;
    private String gender;
    private String classId;
    private String status;
    private String phone;
    private String guardian;
    private String guardianRelation;
    private String guardianPhone;
    private LocalDate birthday;
    private LocalDate enrollmentDate;
    private String address;
    private String dorm;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
