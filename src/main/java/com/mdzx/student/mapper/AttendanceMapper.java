package com.mdzx.student.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.mdzx.student.domain.AttendanceRecord;
import com.mdzx.student.dto.AttendanceDtos;
import com.mdzx.student.mapper.model.AttendanceRow;
import com.mdzx.student.mapper.model.AttendanceStatRow;
import com.mdzx.student.mapper.model.TrendRow;

/**
 * AttendanceMapper。
 * <p>定义数据库读写操作，由 MyBatis XML 提供具体 SQL。</p>
 */
public interface AttendanceMapper {

    /** 分页查询指定日期的考勤记录，并按筛选条件及可见班级范围过滤。 */
    List<AttendanceRow> selectDayPage(
            @Param("date") LocalDate date,
            @Param("grade") String grade,
            @Param("classId") String classId,
            @Param("keyword") String keyword,
            @Param("abnormalOnly") boolean abnormalOnly,
            @Param("classIds") List<String> classIds);

    /** 某天的状态汇总（分母 = 在读学生） */
    AttendanceStatRow selectDaySummary(
            @Param("date") LocalDate date,
            @Param("grade") String grade,
            @Param("classId") String classId,
            @Param("classIds") List<String> classIds);

    /** 统计指定日期已登记考勤的记录数。 */
    long countMarked(@Param("date") LocalDate date);

    /** 批量查询指定学生的考勤统计。 */
    List<AttendanceStatRow> selectStatsByStudentIds(@Param("ids") List<Long> ids);

    /** 查询单个学生的考勤统计。 */
    AttendanceStatRow selectStatsByStudentId(@Param("studentId") Long studentId);

    /** 查询学生最近的考勤记录。 */
    List<AttendanceRecord> selectRecentRecords(@Param("studentId") Long studentId, @Param("size") int size);

    /** 查询日期范围内按日汇总的考勤趋势。 */
    List<TrendRow> selectWeeklyTrend(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("grade") String grade,
            @Param("classId") String classId,
            @Param("classIds") List<String> classIds);

    /** 新增或更新单条学生考勤记录。 */
    int upsert(
            @Param("studentId") Long studentId,
            @Param("date") LocalDate date,
            @Param("status") String status,
            @Param("remark") String remark);

    /** 批量新增或更新指定日期的学生考勤记录。 */
    int batchUpsert(@Param("date") LocalDate date, @Param("list") List<AttendanceDtos.BatchRequest.Item> list);

    /** 删除所有考勤记录。 */
    int deleteAll();

    /** 统计考勤记录总数。 */
    long countAll();
}
