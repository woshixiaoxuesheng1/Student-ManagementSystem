package com.mdzx.student.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.mdzx.student.domain.StudentScore;

/**
 * ScoreMapper。
 * <p>定义数据库读写操作，由 MyBatis XML 提供具体 SQL。</p>
 */
public interface ScoreMapper {

    /** 批量查询指定学生的成绩记录。 */
    List<StudentScore> selectByStudentIds(@Param("ids") List<Long> ids);

    /** 查询指定学生的全部成绩记录。 */
    List<StudentScore> selectByStudentId(@Param("studentId") Long studentId);

    /** 新增或更新一条成绩记录。 */
    int upsert(StudentScore score);

    /** 批量新增或更新成绩记录。 */
    int batchUpsert(@Param("list") List<StudentScore> list);

    /** 删除指定学生的全部成绩记录。 */
    int deleteByStudentId(@Param("studentId") Long studentId);

    /** 删除所有成绩记录。 */
    int deleteAll();
}
