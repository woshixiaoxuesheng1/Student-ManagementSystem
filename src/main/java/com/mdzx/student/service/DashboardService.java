package com.mdzx.student.service;

import java.time.LocalDate;

import com.mdzx.student.vo.DashboardVos;

/**
 * DashboardService。
 * <p>定义业务服务对外提供的操作。</p>
 */
public interface DashboardService {

    DashboardVos.Overview overview(LocalDate date);
}
