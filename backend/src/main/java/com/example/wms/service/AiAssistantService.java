package com.example.wms.service;

import com.example.wms.common.BizException;
import com.example.wms.config.AiProperties;
import com.example.wms.dto.AiAssistantDtos.ChatRequest;
import com.example.wms.dto.AiAssistantDtos.ChatResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class AiAssistantService {
    private final AiProperties properties;
    private final RestClient restClient;

    public AiAssistantService(AiProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    public ChatResponse chat(ChatRequest request) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("session_id", request.sessionId());
            body.put("message", request.message());
            body.put("warehouse_id", request.warehouseId());
            body.put("locale", request.locale() == null ? "zh" : request.locale());
            ChatResponse response = restClient.post()
                    .uri(properties.getServiceUrl() + "/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(ChatResponse.class);
            if (response == null) throw new BizException("AI assistant returned an empty response");
            return response;
        } catch (RestClientException ex) {
            throw new BizException("AI assistant is unavailable: " + ex.getMessage());
        }
    }
}
