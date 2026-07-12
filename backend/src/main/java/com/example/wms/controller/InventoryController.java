package com.example.wms.controller;

import com.example.wms.common.ApiResponse;
import com.example.wms.domain.InventoryCheck;
import com.example.wms.domain.StockMovement;
import com.example.wms.dto.WmsDtos.InboundOrderDetailView;
import com.example.wms.dto.WmsDtos.InboundOrderView;
import com.example.wms.dto.WmsDtos.OrderSearchView;
import com.example.wms.dto.WmsDtos.OrderSummaryView;
import com.example.wms.dto.WmsDtos.OrderQ10mMetricView;
import com.example.wms.dto.WmsDtos.OrderQ10mTimeoutOrderView;
import com.example.wms.dto.WmsDtos.OutboundOrderDetailView;
import com.example.wms.dto.WmsDtos.ReceiveRequest;
import com.example.wms.dto.WmsDtos.ScanLocationView;
import com.example.wms.dto.WmsDtos.ScanProductView;
import com.example.wms.dto.OrderDtos.CheckRequest;
import com.example.wms.dto.OrderDtos.InboundRequest;
import com.example.wms.dto.OrderDtos.OutboundRequest;
import com.example.wms.dto.OrderDtos.PickingRequest;
import com.example.wms.dto.ViewDtos.StockView;
import com.example.wms.repository.InventoryCheckRepository;
import com.example.wms.repository.StockMovementRepository;
import com.example.wms.service.InventoryService;
import com.example.wms.domain.enums.OrderStatus;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    public ApiResponse<List<OrderSummaryView>> inboundOrders(@RequestParam(required = false) Long warehouseId) {
        return ApiResponse.ok(inventoryService.inboundOrders(warehouseId));
    }

    @GetMapping("/inbound/{orderNo}")
    public ApiResponse<InboundOrderDetailView> inboundOrder(@org.springframework.web.bind.annotation.PathVariable String orderNo) {
        return ApiResponse.ok(inventoryService.getInboundOrder(orderNo));
    }

    @GetMapping("/inbound/{orderNo}/receiving")
    public ApiResponse<InboundOrderView> receivableInboundOrder(@org.springframework.web.bind.annotation.PathVariable String orderNo) {
        return ApiResponse.ok(inventoryService.getReceivableInboundOrder(orderNo));
    }

    @PostMapping("/inbound/receive")
    public ApiResponse<InboundOrderView> receive(@RequestBody ReceiveRequest request) {
        return ApiResponse.ok(inventoryService.receiveInbound(request));
    }

    @PostMapping("/inbound/{orderNo}/confirm")
    public ApiResponse<InboundOrderDetailView> confirmInbound(
            @PathVariable String orderNo, @RequestParam(required = false) String operatorName) {
        return ApiResponse.ok(inventoryService.confirmInbound(orderNo, operatorName));
    }

    @PostMapping("/inbound/{orderNo}/cancel")
    public ApiResponse<InboundOrderDetailView> cancelInbound(
            @PathVariable String orderNo, @RequestParam(required = false) String operatorName) {
        return ApiResponse.ok(inventoryService.cancelInbound(orderNo, operatorName));
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
    public ApiResponse<List<OrderSummaryView>> outboundOrders(@RequestParam(required = false) Long warehouseId) {
        return ApiResponse.ok(inventoryService.outboundOrders(warehouseId));
    }

    @PostMapping("/outbound-orders")
    public ApiResponse<OrderSummaryView> createOutboundOrder(@Valid @RequestBody OutboundRequest request) {
        return ApiResponse.ok(inventoryService.outbound(request));
    }

    @GetMapping("/outbound-orders")
    public ApiResponse<List<OrderSummaryView>> outboundOrderList(@RequestParam(required = false) Long warehouseId) {
        return ApiResponse.ok(inventoryService.outboundOrders(warehouseId));
    }

    @GetMapping("/outbound-orders/{id}")
    public ApiResponse<OutboundOrderDetailView> outboundOrderDetail(@PathVariable Long id) {
        return ApiResponse.ok(inventoryService.getOutboundOrder(id));
    }

    @PostMapping("/outbound-orders/{id}/confirm")
    public ApiResponse<OutboundOrderDetailView> confirmOutboundOrder(
            @PathVariable Long id, @RequestParam(required = false) String operatorName) {
        return ApiResponse.ok(inventoryService.confirmOutbound(id, operatorName));
    }

    @PostMapping("/outbound-orders/{id}/picking/start")
    public ApiResponse<OutboundOrderDetailView> startOutboundPicking(
            @PathVariable Long id, @RequestParam(required = false) String operatorName) {
        return ApiResponse.ok(inventoryService.startPicking(id, operatorName));
    }

    @PostMapping("/outbound-orders/{id}/generate-pick-list")
    public ApiResponse<OutboundOrderDetailView> generatePickList(
            @PathVariable Long id, @RequestParam(required = false) String operatorName) {
        return ApiResponse.ok(inventoryService.generatePickList(id, operatorName));
    }

    @PostMapping("/outbound-orders/{id}/assign-picking")
    public ApiResponse<OutboundOrderDetailView> assignPicking(
            @PathVariable Long id, @RequestParam(required = false) String operatorName,
            @RequestParam(defaultValue = "false") boolean continueOnShortage) {
        return ApiResponse.ok(inventoryService.assignPicking(id, operatorName, continueOnShortage));
    }

    @PostMapping("/outbound-orders/{id}/picking/complete")
    public ApiResponse<OutboundOrderDetailView> completeOutboundPicking(
            @PathVariable Long id, @Valid @RequestBody PickingRequest request) {
        return ApiResponse.ok(inventoryService.completePicking(id, request));
    }

    @PostMapping("/outbound-orders/{id}/cancel")
    public ApiResponse<OutboundOrderDetailView> cancelOutboundOrder(
            @PathVariable Long id, @RequestParam(required = false) String operatorName) {
        return ApiResponse.ok(inventoryService.cancelOutbound(id, operatorName));
    }

    @GetMapping("/orders/search")
    public ApiResponse<List<OrderSearchView>> searchOrders(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
            @RequestParam(required = false) String operatorName,
            @RequestParam(required = false) Long warehouseId) {
        return ApiResponse.ok(inventoryService.searchOrders(
                orderNo, direction, status, createdFrom, createdTo, operatorName, warehouseId));
    }

    @GetMapping("/dashboard/order/q-10m-count")
    public ApiResponse<OrderQ10mMetricView> orderQ10mCount() {
        return ApiResponse.ok(inventoryService.orderQ10mCount());
    }

    @GetMapping("/dashboard/order/q-10m-timeout-orders")
    public ApiResponse<List<OrderQ10mTimeoutOrderView>> orderQ10mTimeoutOrders(
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.ok(inventoryService.orderQ10mTimeoutOrders(limit));
    }

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
    public ApiResponse<List<StockMovement>> movements(@RequestParam(required = false) Long warehouseId) {
        return ApiResponse.ok(movementRepository.findTop200ByOrderByMovementTimeDesc().stream()
                .filter(movement -> warehouseId == null || movement.getWarehouse().getId().equals(warehouseId))
                .toList());
    }
}
