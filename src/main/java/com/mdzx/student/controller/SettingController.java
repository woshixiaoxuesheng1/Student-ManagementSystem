package com.mdzx.student.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mdzx.student.common.ApiResponse;
import com.mdzx.student.dto.SettingDtos;
import com.mdzx.student.service.DemoDataService;
import com.mdzx.student.service.SettingService;
import com.mdzx.student.vo.SettingVos;

import jakarta.validation.Valid;

/**
 * SettingController。
 * <p>接收并校验 HTTP 请求，调用业务服务并返回统一接口数据。</p>
 */
@RestController
@RequestMapping("/api/settings")
public class SettingController {

    private final SettingService settingService;
    private final DemoDataService demoDataService;
    public SettingController(SettingService settingService, DemoDataService demoDataService) {
        this.settingService = settingService;
        this.demoDataService = demoDataService;
    }

    /** 读取当前系统设置。 */
    @GetMapping
    public ApiResponse<SettingVos.Setting> get() {
        return ApiResponse.ok(settingService.get());
    }

    /** 校验请求数据并更新指定记录。 */
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SettingVos.Setting> update(@Valid @RequestBody SettingDtos.UpdateRequest request) {
        return ApiResponse.ok(settingService.update(request));
    }

    /** 重置演示数据：清空学籍/成绩/考勤后重新生成 */
    @PostMapping("/reset-demo")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> resetDemo() {
        return ApiResponse.ok(demoDataService.reset());
    }
}
