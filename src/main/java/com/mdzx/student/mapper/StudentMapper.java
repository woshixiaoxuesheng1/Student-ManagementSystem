package com.mdzx.student.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.mdzx.student.domain.Student;
import com.mdzx.student.mapper.model.GradeAggRow;
import com.mdzx.student.mapper.model.GradeStatsRow;
import com.mdzx.student.mapper.model.StudentRow;

/**
 * StudentMapper。
 * <p>定义数据库读写操作，由 MyBatis XML 提供具体 SQL。</p>
 */
public interface StudentMapper {

    /** classIds 传 null 表示不限制（管理员 / 只读账号）；班主任传自己负责的班级 */
    List<StudentRow> selectPage(
            @Param("keyword") String keyword,
            @Param("grade") String grade,
            @Param("classId") String classId,
            @Param("status") String status,
            @Param("absentAlert") boolean absentAlert,
            @Param("incomplete") boolean incomplete,
            @Param("sort") String sort,
            @Param("desc") boolean desc,
            @Param("subjectKey") String subjectKey,
            @Param("classIds") List<String> classIds);

    /** 根据学生 ID 查询学生列表行数据。 */
    StudentRow selectRowById(@Param("id") Long id);

    /** 筛选范围（在读学生）内的成绩聚合 */
    GradeStatsRow selectStats(
            @Param("keyword") String keyword,
            @Param("grade") String grade,
            @Param("classId") String classId,
            @Param("passLine") int passLine,
            @Param("excellentLine") int excellentLine,
            @Param("classIds") List<String> classIds);

    /** 筛选范围内总分最高的学生 */
    StudentRow selectTopByFilter(
            @Param("keyword") String keyword,
            @Param("grade") String grade,
            @Param("classId") String classId,
            @Param("classIds") List<String> classIds);

    /** 返回给定 id 中不属于该班主任班级的学生 id（用于写操作前置校验） */
    List<Long> selectIdsOutsideScope(@Param("ids") List<Long> ids, @Param("classIds") List<String> classIds);

    /** 根据学生 ID 查询学生实体。 */
    Student selectById(@Param("id") Long id);

    /** 新增学生信息。 */
    int insert(Student student);

    /** 更新学生信息。 */
    int update(Student student);

    /** 根据学生 ID 删除学生。 */
    int deleteById(@Param("id") Long id);

    /** 批量删除指定 ID 的学生。 */
    int deleteByIds(@Param("ids") List<Long> ids);

    /** 统计指定学号的学生数量，可排除给定学生 ID。 */
    int countBySid(@Param("sid") String sid, @Param("excludeId") Long excludeId);

    /** 统计可见班级范围内的学生总数。 */
    long countAll(@Param("classIds") List<String> classIds);

    /** 统计指定状态的学生数量。 */
    long countByStatus(@Param("status") String status, @Param("classIds") List<String> classIds);

    /** 统计监护人电话缺失的学生数量。 */
    long countMissingGuardianPhone(@Param("classIds") List<String> classIds);

    /** 统计达到缺勤预警阈值的学生数量。 */
    long countAbsentAlert(@Param("threshold") int threshold, @Param("classIds") List<String> classIds);

    /** 查询最近转班的学生。 */
    List<StudentRow> selectRecentMoves(@Param("size") int size, @Param("classIds") List<String> classIds);

    /** 查询可见班级范围内按年级汇总的学生数据。 */
    List<GradeAggRow> selectGradeAggregates(@Param("classIds") List<String> classIds);

    /** 查询可见班级范围内在读学生的成绩统计。 */
    GradeStatsRow selectActiveStats(@Param("classIds") List<String> classIds);

    /** 删除所有学生记录。 */
    int deleteAll();
}
