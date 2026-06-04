package com.example.wms.controller;

import com.example.wms.common.ApiResponse;
import com.example.wms.common.BizException;
import com.example.wms.domain.StockAlert;
import com.example.wms.domain.enums.AlertStatus;
import com.example.wms.repository.StockAlertRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {
    private final StockAlertRepository alertRepository;

    public AlertController(StockAlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @GetMapping
    public ApiResponse<List<StockAlert>> list(@RequestParam(required = false) AlertStatus status) {
        return ApiResponse.ok(status == null ? alertRepository.findAll() : alertRepository.findByStatus(status));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<StockAlert> status(@PathVariable Long id, @RequestParam AlertStatus status) {
        StockAlert alert = alertRepository.findById(id).orElseThrow(() -> new BizException("预警不存在"));
        alert.setStatus(status);
        return ApiResponse.ok(alertRepository.save(alert));
    }
}
