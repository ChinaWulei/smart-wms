package com.example.wms.controller;

import com.example.wms.common.ApiResponse;
import com.example.wms.domain.AiReport;
import com.example.wms.service.AiReportService;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/reports")
public class AiReportController {
    private final AiReportService aiReportService;

    public AiReportController(AiReportService aiReportService) {
        this.aiReportService = aiReportService;
    }

    @PostMapping
    public ApiResponse<AiReport> generate() {
        return ApiResponse.ok(aiReportService.generate());
    }

    @GetMapping
    public ApiResponse<List<AiReport>> list() {
        return ApiResponse.ok(aiReportService.list());
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("wms-ai-report.pdf").build().toString())
                .body(aiReportService.pdf(id));
    }
}
