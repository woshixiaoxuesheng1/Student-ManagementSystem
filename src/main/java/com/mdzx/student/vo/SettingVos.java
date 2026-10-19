package com.mdzx.student.vo;


import java.time.LocalDateTime;

/** 系统设置的出参 */
public final class SettingVos {

    /** 工具类仅提供静态数据或方法，不需要创建实例。 */
    SettingVos() {
    }

    /** 学校基础信息及系统功能配置。 */
    public record Setting(
            String schoolName,
            String schoolCode,
            String term,
            String principal,
            String address,
            String phone,
            boolean notifyAbsence,
            boolean notifyScore,
            boolean notifyTransfer,
            boolean autoArchive,
            int classCapacity,
            LocalDateTime updatedAt
    ) {
    }
}
