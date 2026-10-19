package com.mdzx.student.service;

import com.mdzx.student.dto.SettingDtos;
import com.mdzx.student.vo.SettingVos;

/**
 * SettingService。
 * <p>定义业务服务对外提供的操作。</p>
 */
public interface SettingService {

    SettingVos.Setting get();

    SettingVos.Setting update(SettingDtos.UpdateRequest request);
}
