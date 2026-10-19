package com.mdzx.student.mapper;

import java.util.List;

import com.mdzx.student.domain.Subject;

/**
 * SubjectMapper。
 * <p>定义数据库读写操作，由 MyBatis XML 提供具体 SQL。</p>
 */
public interface SubjectMapper {

    /** 查询全部科目。 */
    List<Subject> selectAll();
}
