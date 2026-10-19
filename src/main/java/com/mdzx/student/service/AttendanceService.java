package com.mdzx.student.service;

import com.mdzx.student.dto.AttendanceDtos;
import com.mdzx.student.vo.AttendanceVos;

/**
 * AttendanceService。
 * <p>定义业务服务对外提供的操作。</p>
 */
public interface AttendanceService {

    AttendanceVos.Page page(AttendanceDtos.Query query, int page, int size);

    void mark(AttendanceDtos.MarkRequest request);

    int markBatch(AttendanceDtos.BatchRequest request);
}
