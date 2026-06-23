package com.example.wms.service;

import com.example.wms.common.BizException;
import com.example.wms.common.AiServiceUnavailableException;
import com.example.wms.config.AiProperties;
import com.example.wms.dto.AiAssistantDtos.ChatRequest;
import com.example.wms.dto.AiAssistantDtos.ChatResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AiAssistantService {
    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AiAssistantService(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    public ChatResponse chat(ChatRequest request) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("session_id", request.sessionId());
            payload.put("message", request.message());
            payload.put("warehouse_id", request.warehouseId());
            payload.put("locale", request.locale() == null ? "zh" : request.locale());
            String jsonBody = objectMapper.writeValueAsString(payload);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getServiceUrl() + "/chat"))
                    .timeout(Duration.ofSeconds(120))
                    .version(HttpClient.Version.HTTP_1_1)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 503) {
                throw new AiServiceUnavailableException(
                        "AI 模型当前繁忙，系统已自动重试，请稍后再试");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BizException("AI service returned HTTP " + response.statusCode()
                        + ": " + response.body());
            }
            ChatResponse chatResponse = objectMapper.readValue(response.body(), ChatResponse.class);
            if (chatResponse == null) throw new BizException("AI assistant returned an empty response");
            return chatResponse;
        } catch (BizException ex) {
            throw ex;
        } catch (AiServiceUnavailableException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException("AI assistant request was interrupted");
        } catch (Exception ex) {
            throw new BizException("AI assistant is unavailable: " + ex.getMessage());
        }
    }
}
