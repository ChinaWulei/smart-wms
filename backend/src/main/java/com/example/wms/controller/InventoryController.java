package com.example.wms.controller;

import com.example.wms.common.ApiResponse;
import com.example.wms.domain.InventoryCheck;
import com.example.wms.domain.StockMovement;
import com.example.wms.dto.WmsDtos.InboundOrderView;
import com.example.wms.dto.WmsDtos.OrderSummaryView;
import com.example.wms.dto.WmsDtos.ReceiveRequest;
import com.example.wms.dto.WmsDtos.ScanLocationView;
import com.example.wms.dto.WmsDtos.ScanProductView;
import com.example.wms.dto.OrderDtos.CheckRequest;
import com.example.wms.dto.OrderDtos.InboundRequest;
import com.example.wms.dto.OrderDtos.OutboundRequest;
import com.example.wms.dto.ViewDtos.StockView;
import com.example.wms.repository.InventoryCheckRepository;
import com.example.wms.repository.StockMovementRepository;
import com.example.wms.service.InventoryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class InventoryController {
    private final InventoryService inventoryService;
    private final InventoryCheckRepository checkRepository;
    private final StockMovementRepository movementRepository;

    public InventoryController(InventoryService inventoryService, InventoryCheckRepository checkRepository,
                               StockMovementRepository movementRepository) {
        this.inventoryService = inventoryService;
        this.checkRepository = checkRepository;
        this.movementRepository = movementRepository;
    }

    @PostMapping("/inbound")
    public ApiResponse<OrderSummaryView> inbound(@Valid @RequestBody InboundRequest request) { return ApiResponse.ok(inventoryService.inbound(request)); }

    @GetMapping("/inbound")
    public ApiResponse<List<OrderSummaryView>> inboundOrders() { return ApiResponse.ok(inventoryService.inboundOrders()); }

    @GetMapping("/inbound/{orderNo}")
    public ApiResponse<InboundOrderView> inboundOrder(@org.springframework.web.bind.annotation.PathVariable String orderNo) {
        return ApiResponse.ok(inventoryService.getInboundOrder(orderNo));
    }

    @PostMapping("/inbound/receive")
    public ApiResponse<InboundOrderView> receive(@RequestBody ReceiveRequest request) {
        return ApiResponse.ok(inventoryService.receiveInbound(request));
    }

    @GetMapping("/scan/location/{code}")
    public ApiResponse<ScanLocationView> scanLocation(@org.springframework.web.bind.annotation.PathVariable String code) {
        return ApiResponse.ok(inventoryService.scanLocation(code));
    }

    @GetMapping("/scan/inbound-product")
    public ApiResponse<ScanProductView> scanInboundProduct(@RequestParam String orderNo, @RequestParam String code) {
        return ApiResponse.ok(inventoryService.scanInboundProduct(orderNo, code));
    }

    @PostMapping("/outbound")
    public ApiResponse<OrderSummaryView> outbound(@Valid @RequestBody OutboundRequest request) { return ApiResponse.ok(inventoryService.outbound(request)); }

    @GetMapping("/outbound")
    public ApiResponse<List<OrderSummaryView>> outboundOrders() { return ApiResponse.ok(inventoryService.outboundOrders()); }

    @PostMapping("/inventory-checks")
    public ApiResponse<InventoryCheck> check(@Valid @RequestBody CheckRequest request) { return ApiResponse.ok(inventoryService.confirmCheck(request)); }

    @GetMapping("/inventory-checks")
    public ApiResponse<List<InventoryCheck>> checks() { return ApiResponse.ok(checkRepository.findAll()); }

    @GetMapping("/stocks")
    public ApiResponse<List<StockView>> stocks(@RequestParam(required = false) Long productId,
                                               @RequestParam(required = false) Long warehouseId,
                                               @RequestParam(required = false) Long locationId) {
        return ApiResponse.ok(inventoryService.queryStocks(productId, warehouseId, locationId));
    }

    @GetMapping("/movements")
    public ApiResponse<List<StockMovement>> movements() { return ApiResponse.ok(movementRepository.findTop200ByOrderByMovementTimeDesc()); }
}
