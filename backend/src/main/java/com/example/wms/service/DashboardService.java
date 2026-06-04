package com.example.wms.service;

import com.example.wms.domain.Stock;
import com.example.wms.domain.enums.AlertStatus;
import com.example.wms.repository.ProductRepository;
import com.example.wms.repository.StockAlertRepository;
import com.example.wms.repository.StockMovementRepository;
import com.example.wms.repository.StockRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final StockMovementRepository movementRepository;
    private final StockAlertRepository alertRepository;

    public DashboardService(ProductRepository productRepository, StockRepository stockRepository,
                            StockMovementRepository movementRepository, StockAlertRepository alertRepository) {
        this.productRepository = productRepository;
        this.stockRepository = stockRepository;
        this.movementRepository = movementRepository;
        this.alertRepository = alertRepository;
    }

    public Map<String, Object> dashboard() {
        LocalDateTime today = LocalDate.now().atStartOfDay();
        LocalDateTime thirtyDaysAgo = LocalDate.now().minusDays(30).atStartOfDay();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("productTotal", productRepository.count());
        data.put("stockTotal", stockRepository.totalQuantity());
        data.put("todayInbound", movementRepository.todayInbound(today));
        data.put("todayOutbound", movementRepository.todayOutbound(today));
        data.put("warningCount", alertRepository.countByStatusNot(AlertStatus.RESOLVED));
        data.put("topOutboundProducts", movementRepository.topOutbound(thirtyDaysAgo).stream().limit(8)
                .map(row -> Map.of("name", row[0], "quantity", row[1])).toList());
        data.put("stockTrend", movementRepository.trend(thirtyDaysAgo).stream()
                .map(row -> Map.of("date", String.valueOf(row[0]), "inbound", row[1], "outbound", row[2])).toList());
        data.put("warehouseShare", stockRepository.findAll().stream()
                .collect(Collectors.groupingBy(s -> s.getWarehouse().getName(), Collectors.summingInt(Stock::getQuantity)))
                .entrySet().stream().map(e -> Map.of("warehouse", e.getKey(), "quantity", e.getValue())).toList());
        return data;
    }

    public Map<String, Object> structuredAiData() {
        Map<String, Object> data = dashboard();
        data.put("stocks", stockRepository.findAll().stream().map(s -> Map.of(
                "sku", s.getProduct().getSku(),
                "productName", s.getProduct().getName(),
                "category", value(s.getProduct().getCategory()),
                "warehouse", s.getWarehouse().getName(),
                "location", s.getLocation().getCode(),
                "quantity", s.getQuantity(),
                "safetyStock", s.getProduct().getSafetyStock(),
                "supplier", value(s.getProduct().getSupplier())
        )).toList());
        data.put("recentMovements", movementRepository.findTop200ByOrderByMovementTimeDesc().stream().map(m -> Map.of(
                "productName", m.getProduct().getName(),
                "type", m.getType().name(),
                "quantity", m.getQuantity(),
                "warehouse", m.getWarehouse().getName(),
                "time", String.valueOf(m.getMovementTime())
        )).toList());
        return data;
    }

    private String value(String text) {
        return text == null ? "" : text;
    }
}
