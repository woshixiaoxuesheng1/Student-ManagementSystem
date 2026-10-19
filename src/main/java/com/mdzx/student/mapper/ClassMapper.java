package com.mdzx.student.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.mdzx.student.domain.ClassInfo;
import com.mdzx.student.mapper.model.ClassAggRow;
import com.mdzx.student.mapper.model.StudentBriefRow;

/**
 * ClassMapper。
 * <p>定义数据库读写操作，由 MyBatis XML 提供具体 SQL。</p>
 */
public interface ClassMapper {

    /** 查询指定年级及班级范围内的班级列表。 */
    List<ClassInfo> selectAll(@Param("grade") String grade, @Param("classIds") List<String> classIds);

    /** 根据班级编号查询班级信息。 */
    ClassInfo selectById(@Param("classId") String classId);

    /** 批量查询指定班级的统计汇总。 */
    List<ClassAggRow> selectAggregates(@Param("classIds") List<String> classIds);

    /** 查询指定班级中成绩排名靠前的学生。 */
    List<StudentBriefRow> selectTopStudents(@Param("classIds") List<String> classIds);

    /** 新增班级信息。 */
    int insert(ClassInfo classInfo);

    /** 更新指定班级的容量。 */
    int updateCapacity(@Param("classId") String classId, @Param("capacity") int capacity);
}
