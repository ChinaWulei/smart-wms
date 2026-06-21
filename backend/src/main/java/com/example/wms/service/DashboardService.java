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

    public Map<String, Object> dashboard(Long warehouseId) {
        LocalDateTime today = LocalDate.now().atStartOfDay();
        LocalDateTime thirtyDaysAgo = LocalDate.now().minusDays(30).atStartOfDay();
        List<Stock> warehouseStocks = stockRepository.findAll().stream()
                .filter(stock -> warehouseId == null || stock.getWarehouse().getId().equals(warehouseId))
                .toList();
        var warehouseMovements = movementRepository.findTop200ByOrderByMovementTimeDesc().stream()
                .filter(movement -> warehouseId == null || movement.getWarehouse().getId().equals(warehouseId))
                .toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("productTotal", productRepository.count());
        data.put("stockTotal", warehouseStocks.stream().mapToInt(Stock::getQuantity).sum());
        data.put("todayInbound", warehouseMovements.stream()
                .filter(movement -> movement.getMovementTime() != null && !movement.getMovementTime().isBefore(today))
                .filter(movement -> movement.getType().name().equals("INBOUND"))
                .mapToInt(movement -> movement.getQuantity() == null ? 0 : movement.getQuantity()).sum());
        data.put("todayOutbound", warehouseMovements.stream()
                .filter(movement -> movement.getMovementTime() != null && !movement.getMovementTime().isBefore(today))
                .filter(movement -> movement.getType().name().equals("OUTBOUND"))
                .mapToInt(movement -> movement.getQuantity() == null ? 0 : movement.getQuantity()).sum());
        data.put("warningCount", alertRepository.countByStatusNot(AlertStatus.RESOLVED));
        data.put("topOutboundProducts", movementRepository.topOutbound(thirtyDaysAgo).stream().limit(8)
                .map(row -> Map.of("name", row[0], "quantity", row[1])).toList());
        data.put("stockTrend", movementRepository.trend(thirtyDaysAgo).stream()
                .map(row -> Map.of("date", String.valueOf(row[0]), "inbound", row[1], "outbound", row[2])).toList());
        data.put("warehouseShare", warehouseStocks.stream()
                .collect(Collectors.groupingBy(s -> s.getWarehouse().getName(), Collectors.summingInt(Stock::getQuantity)))
                .entrySet().stream().map(e -> Map.of("warehouse", e.getKey(), "quantity", e.getValue())).toList());
        return data;
    }

    public Map<String, Object> structuredAiData() {
        return structuredAiData(null);
    }

    public Map<String, Object> structuredAiData(Long warehouseId) {
        Map<String, Object> data = dashboard(warehouseId);
        data.put("stocks", stockRepository.findAll().stream()
                .filter(s -> warehouseId == null || s.getWarehouse().getId().equals(warehouseId))
                .map(s -> Map.of(
                "sku", s.getProduct().getSku(),
                "productName", s.getProduct().getName(),
                "category", value(s.getProduct().getCategory()),
                "warehouse", s.getWarehouse().getName(),
                "location", s.getLocation().getCode(),
                "quantity", s.getQuantity(),
                "safetyStock", s.getProduct().getSafetyStock(),
                "supplier", value(s.getProduct().getSupplier())
        )).toList());
        data.put("recentMovements", movementRepository.findTop200ByOrderByMovementTimeDesc().stream()
                .filter(m -> warehouseId == null || m.getWarehouse().getId().equals(warehouseId))
                .map(m -> Map.of(
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
