package com.example.wms.dto;

import com.example.wms.domain.enums.StockStatus;

public class ViewDtos {
    public record StockView(Long stockId, String sku, String productName, String warehouseName, String locationCode,
                            Integer quantity, Integer safetyStock, StockStatus status) {}
}
