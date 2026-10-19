package com.mdzx.student.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mdzx.student.common.ApiResponse;
import com.mdzx.student.service.DashboardService;
import com.mdzx.student.vo.DashboardVos;

/**
 * DashboardController。
 * <p>接收并校验 HTTP 请求，调用业务服务并返回统一接口数据。</p>
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /** 概览：指标、趋势、待办、年级汇总、近期异动 */
    @GetMapping("/overview")
    public ApiResponse<DashboardVos.Overview> overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(dashboardService.overview(date));
    }
}
