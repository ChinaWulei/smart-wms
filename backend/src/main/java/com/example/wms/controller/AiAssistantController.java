package com.example.wms.controller;

import com.example.wms.common.ApiResponse;
import com.example.wms.dto.AiAssistantDtos.ChatRequest;
import com.example.wms.dto.AiAssistantDtos.ChatResponse;
import com.example.wms.service.AiAssistantService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import java.io.InputStream;

@RestController
@RequestMapping("/api/ai/assistant")
public class AiAssistantController {
    private final AiAssistantService aiAssistantService;

    public AiAssistantController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ApiResponse.ok(aiAssistantService.chat(request));
    }

    @PostMapping(value = "/chat/stream", produces = "application/x-ndjson")
    public StreamingResponseBody chatStream(@Valid @RequestBody ChatRequest request) {
        return outputStream -> {
            try (InputStream inputStream = aiAssistantService.chatStream(request).body()) {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                    outputStream.flush();
                }
            }
        };
    }
}
