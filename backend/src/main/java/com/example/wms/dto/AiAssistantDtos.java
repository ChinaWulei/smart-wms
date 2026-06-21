package com.example.wms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AiAssistantDtos {
    private AiAssistantDtos() {}

    public record ChatRequest(
            @NotBlank String sessionId,
            @NotBlank @Size(max = 4000) String message,
            Long warehouseId,
            String locale
    ) {}

    public record ChatResponse(
            String answer,
            String agent,
            Long reportId,
            String downloadUrl
    ) {}

    public record SaveReportRequest(
            @NotBlank String title,
            @NotBlank String analysisText,
            @NotBlank String structuredDataJson
    ) {}
}
