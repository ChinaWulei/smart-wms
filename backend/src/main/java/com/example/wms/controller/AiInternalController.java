package com.example.wms.controller;

import com.example.wms.common.ApiResponse;
import com.example.wms.common.BizException;
import com.example.wms.config.AiProperties;
import com.example.wms.domain.AiReport;
import com.example.wms.dto.AiAssistantDtos.SaveReportRequest;
import com.example.wms.service.AiReportService;
import com.example.wms.service.DashboardService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/ai")
public class AiInternalController {
    private final DashboardService dashboardService;
    private final AiReportService aiReportService;
    private final AiProperties properties;

    public AiInternalController(DashboardService dashboardService, AiReportService aiReportService,
                                AiProperties properties) {
        this.dashboardService = dashboardService;
        this.aiReportService = aiReportService;
        this.properties = properties;
    }

    @GetMapping("/context")
    public ApiResponse<Map<String, Object>> context(
            @RequestHeader("X-AI-Internal-Token") String token,
            @RequestParam(required = false) Long warehouseId) {
        authorize(token);
        return ApiResponse.ok(dashboardService.structuredAiData(warehouseId));
    }

    @PostMapping("/reports")
    public ApiResponse<AiReport> saveReport(
            @RequestHeader("X-AI-Internal-Token") String token,
            @Valid @RequestBody SaveReportRequest request) {
        authorize(token);
        return ApiResponse.ok(aiReportService.createFromAgent(
                request.title(), request.analysisText(), request.structuredDataJson()));
    }

    private void authorize(String token) {
        if (properties.getInternalToken() == null || !properties.getInternalToken().equals(token)) {
            throw new BizException("Invalid AI internal token");
        }
    }
}
