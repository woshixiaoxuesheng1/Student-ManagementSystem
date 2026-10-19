package com.mdzx.student.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mdzx.student.common.BizException;
import com.mdzx.student.common.ErrorCode;
import com.mdzx.student.domain.ClassInfo;
import com.mdzx.student.domain.SchoolSetting;
import com.mdzx.student.dto.SettingDtos;
import com.mdzx.student.mapper.ClassMapper;
import com.mdzx.student.mapper.SettingMapper;
import com.mdzx.student.service.SettingService;
import com.mdzx.student.vo.SettingVos;

/**
 * SettingService。
 * <p>实现对应业务流程，组合数据访问、校验与权限规则。</p>
 */
@Service
public class SettingServiceImpl implements SettingService {

    private final SettingMapper settingMapper;
    private final ClassMapper classMapper;
    public SettingServiceImpl(SettingMapper settingMapper, ClassMapper classMapper) {
        this.settingMapper = settingMapper;
        this.classMapper = classMapper;
    }

    /** 读取当前系统设置。 */
    @Override
    public SettingVos.Setting get() {
        return toVo(requireSetting());
    }

    /** 校验请求数据并更新指定记录。 */
    @Override
    @Transactional
    public SettingVos.Setting update(SettingDtos.UpdateRequest request) {
        SchoolSetting current = requireSetting();
        current.setSchoolName(request.schoolName());
        current.setSchoolCode(request.schoolCode());
        current.setTerm(request.term());
        current.setPrincipal(request.principal());
        current.setAddress(request.address());
        current.setPhone(request.phone());
        current.setNotifyAbsence(request.notifyAbsence() == null ? current.getNotifyAbsence() : request.notifyAbsence());
        current.setNotifyScore(request.notifyScore() == null ? current.getNotifyScore() : request.notifyScore());
        current.setNotifyTransfer(
                request.notifyTransfer() == null ? current.getNotifyTransfer() : request.notifyTransfer());
        current.setAutoArchive(request.autoArchive() == null ? current.getAutoArchive() : request.autoArchive());
        if (request.classCapacity() != null) {
            current.setClassCapacity(request.classCapacity());
            for (ClassInfo info : classMapper.selectAll(null, null)) {
                classMapper.updateCapacity(info.getClassId(), request.classCapacity());
            }
        }
        settingMapper.update(current);
        return toVo(requireSetting());
    }

    /** 读取学校设置；记录不存在时抛出业务异常。 */
    private SchoolSetting requireSetting() {
        SchoolSetting setting = settingMapper.select();
        if (setting == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "未初始化学校信息，请先执行 db/schema.sql");
        }
        return setting;
    }

    /** 将学校设置实体转换为接口响应对象。 */
    private SettingVos.Setting toVo(SchoolSetting setting) {
        return new SettingVos.Setting(
                setting.getSchoolName(),
                setting.getSchoolCode(),
                setting.getTerm(),
                setting.getPrincipal(),
                setting.getAddress(),
                setting.getPhone(),
                Boolean.TRUE.equals(setting.getNotifyAbsence()),
                Boolean.TRUE.equals(setting.getNotifyScore()),
                Boolean.TRUE.equals(setting.getNotifyTransfer()),
                Boolean.TRUE.equals(setting.getAutoArchive()),
                setting.getClassCapacity() == null ? 0 : setting.getClassCapacity(),
                setting.getUpdatedAt());
    }
}
