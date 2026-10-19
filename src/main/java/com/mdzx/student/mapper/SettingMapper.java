package com.mdzx.student.mapper;

import com.mdzx.student.domain.SchoolSetting;

/**
 * SettingMapper。
 * <p>定义数据库读写操作，由 MyBatis XML 提供具体 SQL。</p>
 */
public interface SettingMapper {

    /** 查询学校设置。 */
    SchoolSetting select();

    /** 更新学校设置。 */
    int update(SchoolSetting setting);
}
