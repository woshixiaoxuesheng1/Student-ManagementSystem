package com.mdzx.student.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;


/**
 * SchoolSetting。
 * <p>表示系统中的持久化业务实体及其字段。</p>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class SchoolSetting {

    private Integer id;
    private String schoolName;
    private String schoolCode;
    private String term;
    private String principal;
    private String address;
    private String phone;
    private Boolean notifyAbsence;
    private Boolean notifyScore;
    private Boolean notifyTransfer;
    private Boolean autoArchive;
    private Integer classCapacity;
    private LocalDateTime updatedAt;
}
