package com.mdzx.student.service;

import java.util.List;

import com.mdzx.student.common.PageResult;
import com.mdzx.student.dto.StudentDtos;
import com.mdzx.student.vo.StudentVos;

/**
 * StudentService。
 * <p>定义业务服务对外提供的操作。</p>
 */
public interface StudentService {

    PageResult<StudentVos.Item> page(StudentDtos.Query query, int page, int size);

    StudentVos.Detail detail(Long id);

    StudentVos.Item create(StudentDtos.SaveRequest request);

    StudentVos.Item update(Long id, StudentDtos.SaveRequest request);

    void delete(Long id);

    int deleteBatch(List<Long> ids);

    StudentVos.Options options();
}
