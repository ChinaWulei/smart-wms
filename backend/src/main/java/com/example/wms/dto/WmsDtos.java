package com.example.wms.dto;

import com.example.wms.domain.enums.LocationStatus;
import com.example.wms.domain.enums.OrderStatus;
import java.util.List;

public class WmsDtos {
    public record LoginRequest(String username, String password) {}
    public record ShelfGenerateRequest(Long warehouseId, String shelfCode, String shelfName, Integer xCount,
                                       Integer yCount, Integer zCount, Integer capacity, String remark) {}
    public record ShelfPreviewResponse(List<String> codes, Integer total) {}
    public record LocationView(Long id, String code, Long warehouseId, String warehouseName, Long shelfId,
                               String shelfCode, Integer capacity, Integer occupied, LocationStatus status) {}
    public record ReceiveRequest(String orderNo, String locationCode, String productCode, Integer quantity,
                                 Boolean allowOverReceive, String operatorName) {}
    public record InboundItemView(Long itemId, String sku, String barcode, String productName, String modelSpec,
                                  String unitName, Integer expectedQuantity, Integer receivedQuantity,
                                  Integer remainingQuantity, String receiveStatus) {}
    public record InboundOrderView(String orderNo, String supplier, String type, OrderStatus status,
                                   Integer expectedTotal, Integer receivedTotal, Integer progress,
                                   List<InboundItemView> items) {}
    public record OrderSummaryView(Long id, String orderNo, String type, OrderStatus status, String operatorName,
                                   String remark, Integer itemCount) {}
    public record ScanLocationView(String code, String warehouseName, String shelfCode, LocationStatus status,
                                   Integer capacity, Integer occupied) {}
    public record ScanProductView(String sku, String barcode, String productName, String modelSpec, String unitName,
                                  Integer expectedQuantity, Integer receivedQuantity, Integer remainingQuantity) {}
}
