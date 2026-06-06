package com.example.wms.controller;

import com.example.wms.common.ApiResponse;
import com.example.wms.service.DashboardService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> dashboard(@RequestParam(required = false) Long warehouseId) {
        return ApiResponse.ok(dashboardService.dashboard(warehouseId));
    }
}
