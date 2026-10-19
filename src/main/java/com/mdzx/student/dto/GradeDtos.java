package com.mdzx.student.dto;


/** 成绩模块的入参 */
public final class GradeDtos {

    /** 工具类仅提供静态数据或方法，不需要创建实例。 */
    GradeDtos() {
    }

    /** 成绩列表的筛选条件和排序方式。 */
    public record Query(
            String grade,
            String classId,
            String keyword,
            String sort,
            String dir
    ) {
    /** 根据排序方向参数判断是否采用降序。 */
        public boolean desc() {
            return !"asc".equalsIgnoreCase(dir);
        }
    
    
    }
}
