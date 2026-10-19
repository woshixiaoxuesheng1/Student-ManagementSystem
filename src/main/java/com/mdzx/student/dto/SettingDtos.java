package com.mdzx.student.dto;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 系统设置的入参 */
public final class SettingDtos {

    /** 工具类仅提供静态数据或方法，不需要创建实例。 */
    SettingDtos() {
    }

    /** 修改学校基础信息和系统配置时提交的数据。 */
    public record UpdateRequest(
            @NotBlank(message = "请填写学校名称") @Size(max = 64) String schoolName,
            @Size(max = 32) String schoolCode,
            @Size(max = 64) String term,
            @Size(max = 32) String principal,
            @Size(max = 128) String address,
            @Size(max = 32) String phone,
            Boolean notifyAbsence,
            Boolean notifyScore,
            Boolean notifyTransfer,
            Boolean autoArchive,
            @Min(value = 1, message = "班级人数上限至少为 1") @Max(value = 100, message = "班级人数上限过大") Integer classCapacity
    ) {
    }
}
