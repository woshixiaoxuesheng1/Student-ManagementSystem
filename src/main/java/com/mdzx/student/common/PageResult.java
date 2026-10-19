package com.mdzx.student.common;

import java.util.List;

import com.github.pagehelper.Page;

/** 统一分页结果，包含当前页数据、总数和分页信息。 */
public record PageResult<T>(List<T> list, long total, int page, int size, int pages) {

    /** 根据列表、总数和分页参数创建分页结果。 */
    public static <T> PageResult<T> of(List<T> list, long total, int page, int size) {
        int pages = size <= 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PageResult<>(list, total, page, size, pages);
    }

    /** 将 PageHelper 查询结果转换为统一分页响应。 */
    public static <T> PageResult<T> of(Page<?> page, List<T> list) {
        return of(list, page.getTotal(), page.getPageNum(), page.getPageSize());
    }
}
