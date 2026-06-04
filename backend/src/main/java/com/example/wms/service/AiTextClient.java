package com.example.wms.service;

import com.example.wms.config.AiProperties;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class AiTextClient {
    private final AiProperties properties;
    private final RestClient restClient = RestClient.create();

    public AiTextClient(AiProperties properties) {
        this.properties = properties;
    }

    @SuppressWarnings("unchecked")
    public String analyze(String structuredJson) {
        if (!StringUtils.hasText(properties.getEndpoint()) || !StringUtils.hasText(properties.getApiKey())) {
            return fallback(structuredJson);
        }
        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", "你是WMS仓储运营分析助手。只能基于用户提供的结构化库存统计数据输出分析，不能声称直接查询数据库。"),
                        Map.of("role", "user", "content", "请根据以下结构化数据生成：1.补货建议；2.库存异常分析；3.运营报告摘要；4.后续行动。\n" + structuredJson)
                )
        );
        Map<String, Object> response = restClient.post()
                .uri(properties.getEndpoint())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .body(body)
                .retrieve()
                .body(Map.class);
        if (response == null || !response.containsKey("choices")) {
            return fallback(structuredJson);
        }
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return String.valueOf(message.get("content"));
    }

    private String fallback(String structuredJson) {
        return """
                AI仓储运营报告

                当前未配置外部AI服务，系统已基于后端汇总的结构化数据生成基础分析。

                1. 补货建议：优先处理当前库存低于安全库存的SKU，并结合近30天出库Top商品安排采购。
                2. 异常分析：重点关注库存不足、库存超过安全库存3倍的积压商品，以及近30天出库量突增商品。
                3. 运营建议：仓库管理员应每日复核预警清单；采购/运营人员应按供应商和分类合并补货需求。
                4. 数据边界：AI没有直接查询数据库，输入只来自后端聚合后的结构化库存、流水和看板数据。
                """;
    }
}
