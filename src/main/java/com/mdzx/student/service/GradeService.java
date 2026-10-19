package com.mdzx.student.service;

import com.mdzx.student.dto.GradeDtos;
import com.mdzx.student.vo.GradeVos;

/**
 * GradeService。
 * <p>定义业务服务对外提供的操作。</p>
 */
public interface GradeService {

    GradeVos.Page page(GradeDtos.Query query, int page, int size);
}
