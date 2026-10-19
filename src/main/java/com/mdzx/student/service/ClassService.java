package com.mdzx.student.service;

import java.util.List;

import com.mdzx.student.vo.ClassVos;

/**
 * ClassService。
 * <p>定义业务服务对外提供的操作。</p>
 */
public interface ClassService {

    List<ClassVos.Summary> list(String grade, String sort);
}
