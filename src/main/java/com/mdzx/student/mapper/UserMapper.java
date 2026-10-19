package com.mdzx.student.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.mdzx.student.domain.SysUser;
import org.apache.ibatis.annotations.Select;

/**
 * UserMapper。
 * <p>定义数据库读写操作，由 MyBatis XML 提供具体 SQL。</p>
 */
public interface UserMapper {

    /** 根据用户名查询系统用户。 */
    SysUser selectByUsername(@Param("username") String username);

    /** 根据用户 ID 查询系统用户。 */
    SysUser selectById(@Param("id") Long id);

    /** 查询用户负责的班级编号列表。 */
    @Select("SELECT class_id FROM sys_user_class WHERE user_id = #{userId} ORDER BY class_id\n")
    List<String> selectClassIds(@Param("userId") Long userId);

    /** 查询全部系统用户。 */
    List<SysUser> selectAll();

    /** 批量查询指定用户关联的班级编号。 */
    List<String> selectClassIdsByUserIds(@Param("userIds") List<Long> userIds);

    /** 新增系统用户。 */
    int insert(SysUser user);

    /** 批量新增用户与班级的关联关系。 */
    int insertUserClasses(@Param("userId") Long userId, @Param("classIds") List<String> classIds);

    /**更新用户最近登录时间。*/
    void updateLastLogin(@Param("id") Long id);

    /** 统计系统用户总数。 */
    long countAll();
}
