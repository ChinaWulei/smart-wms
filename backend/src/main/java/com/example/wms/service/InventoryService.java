package com.example.wms.service;

import com.example.wms.common.BizException;
import com.example.wms.domain.InboundOrder;
import com.example.wms.domain.InboundOrderItem;
import com.example.wms.domain.InventoryCheck;
import com.example.wms.domain.InventoryCheckItem;
import com.example.wms.domain.OutboundOrder;
import com.example.wms.domain.OutboundOrderItem;
import com.example.wms.domain.Product;
import com.example.wms.domain.Stock;
import com.example.wms.domain.StockAlert;
import com.example.wms.domain.StockMovement;
import com.example.wms.domain.StorageLocation;
import com.example.wms.domain.Warehouse;
import com.example.wms.domain.enums.AlertStatus;
import com.example.wms.domain.enums.LocationStatus;
import com.example.wms.domain.enums.MovementType;
import com.example.wms.domain.enums.OrderStatus;
import com.example.wms.domain.enums.StockStatus;
import com.example.wms.dto.OrderDtos.CheckLineItem;
import com.example.wms.dto.OrderDtos.CheckRequest;
import com.example.wms.dto.OrderDtos.InboundRequest;
import com.example.wms.dto.OrderDtos.LineItem;
import com.example.wms.dto.OrderDtos.OutboundRequest;
import com.example.wms.dto.ViewDtos.StockView;
import com.example.wms.dto.WmsDtos.InboundItemView;
import com.example.wms.dto.WmsDtos.InboundOrderDetailView;
import com.example.wms.dto.WmsDtos.InboundOrderView;
import com.example.wms.dto.WmsDtos.OrderHistoryView;
import com.example.wms.dto.WmsDtos.OrderSummaryView;
import com.example.wms.dto.WmsDtos.ReceiveRequest;
import com.example.wms.dto.WmsDtos.ScanLocationView;
import com.example.wms.dto.WmsDtos.ScanProductView;
import com.example.wms.repository.InboundOrderRepository;
import com.example.wms.repository.InventoryCheckRepository;
import com.example.wms.repository.OutboundOrderRepository;
import com.example.wms.repository.ProductRepository;
import com.example.wms.repository.StockAlertRepository;
import com.example.wms.repository.StockMovementRepository;
import com.example.wms.repository.StockRepository;
import com.example.wms.repository.StorageLocationRepository;
import com.example.wms.repository.WarehouseRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final StorageLocationRepository locationRepository;
    private final StockRepository stockRepository;
    private final StockMovementRepository movementRepository;
    private final StockAlertRepository alertRepository;
    private final InboundOrderRepository inboundOrderRepository;
    private final OutboundOrderRepository outboundOrderRepository;
    private final InventoryCheckRepository checkRepository;

    public InventoryService(ProductRepository productRepository, WarehouseRepository warehouseRepository,
                            StorageLocationRepository locationRepository, StockRepository stockRepository,
                            StockMovementRepository movementRepository, StockAlertRepository alertRepository,
                            InboundOrderRepository inboundOrderRepository, OutboundOrderRepository outboundOrderRepository,
                            InventoryCheckRepository checkRepository) {
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.locationRepository = locationRepository;
        this.stockRepository = stockRepository;
        this.movementRepository = movementRepository;
        this.alertRepository = alertRepository;
        this.inboundOrderRepository = inboundOrderRepository;
        this.outboundOrderRepository = outboundOrderRepository;
        this.checkRepository = checkRepository;
    }

    @Transactional
    public OrderSummaryView inbound(InboundRequest request) {
        InboundOrder order = new InboundOrder();
        order.setOrderNo("IN" + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now()));
        order.setType(request.type());
        order.setOperatorName(request.operatorName());
        order.setRemark(request.remark());
        int itemIndex = 1;
        for (LineItem line : request.items()) {
            Product product = product(line.productId());
            Warehouse warehouse = warehouse(line.warehouseId());
            StorageLocation location = location(line.locationId());
            validateLocationWarehouse(location, warehouse);
            InboundOrderItem item = new InboundOrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setWarehouse(warehouse);
            item.setLocation(location);
            item.setQuantity(line.quantity());
            item.setReceivedQuantity(0);
            item.setTrackingNo(trackingNo(order.getOrderNo(), itemIndex++));
            order.getItems().add(item);
        }
        order.setStatus(OrderStatus.CREATED);
        return inboundSummary(inboundOrderRepository.save(order));
    }

    @Transactional
    public OrderSummaryView outbound(OutboundRequest request) {
        OutboundOrder order = new OutboundOrder();
        order.setOrderNo("OUT" + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now()));
        order.setType(request.type());
        order.setOperatorName(request.operatorName());
        order.setRemark(request.remark());
        for (LineItem line : request.items()) {
            Product product = product(line.productId());
            Warehouse warehouse = warehouse(line.warehouseId());
            StorageLocation location = location(line.locationId());
            validateLocationWarehouse(location, warehouse);
            OutboundOrderItem item = new OutboundOrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setWarehouse(warehouse);
            item.setLocation(location);
            item.setQuantity(line.quantity());
            order.getItems().add(item);
        }
        order.setStatus(OrderStatus.CREATED);
        return outboundSummary(outboundOrderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryView> inboundOrders() {
        return inboundOrderRepository.findAll().stream().map(this::inboundSummary).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryView> outboundOrders() {
        return outboundOrderRepository.findAll().stream().map(this::outboundSummary).toList();
    }

    @Transactional
    public InventoryCheck confirmCheck(CheckRequest request) {
        InventoryCheck task = new InventoryCheck();
        task.setCheckNo("CHK" + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now()));
        task.setOperatorName(request.operatorName());
        task.setRemark(request.remark());
        for (CheckLineItem line : request.items()) {
            Product product = product(line.productId());
            Warehouse warehouse = warehouse(line.warehouseId());
            StorageLocation location = location(line.locationId());
            Stock stock = findOrCreateStock(product, warehouse, location);
            int book = stock.getQuantity();
            int diff = line.actualQuantity() - book;
            if (diff != 0) {
                adjustStock(product, warehouse, location, diff, diff > 0 ? MovementType.CHECK_GAIN : MovementType.CHECK_LOSS,
                        task.getCheckNo(), request.operatorName());
            }
            InventoryCheckItem item = new InventoryCheckItem();
            item.setCheckTask(task);
            item.setProduct(product);
            item.setWarehouse(warehouse);
            item.setLocation(location);
            item.setBookQuantity(book);
            item.setActualQuantity(line.actualQuantity());
            item.setDiffQuantity(diff);
            task.getItems().add(item);
        }
        task.setStatus(OrderStatus.COMPLETED);
        task.setConfirmedAt(LocalDateTime.now());
        return checkRepository.save(task);
    }

    public List<StockView> queryStocks(Long productId, Long warehouseId, Long locationId) {
        return stockRepository.findAll().stream()
                .filter(s -> productId == null || s.getProduct().getId().equals(productId))
                .filter(s -> warehouseId == null || s.getWarehouse().getId().equals(warehouseId))
                .filter(s -> locationId == null || s.getLocation().getId().equals(locationId))
                .map(s -> new StockView(s.getId(), s.getProduct().getSku(), s.getProduct().getName(), s.getWarehouse().getName(),
                        s.getLocation().getCode(), s.getQuantity(), s.getProduct().getSafetyStock(),
                        status(s.getQuantity(), s.getProduct().getSafetyStock())))
                .toList();
    }

    @Transactional(readOnly = true)
    public InboundOrderDetailView getInboundOrder(String orderNo) {
        return inboundDetail(inboundOrderRepository.findByOrderNo(orderNo).orElseThrow(() -> new BizException("入库单不存在")));
    }

    @Transactional(readOnly = true)
    public InboundOrderView getReceivableInboundOrder(String orderNo) {
        InboundOrder order = inboundOrderRepository.findByOrderNo(orderNo).orElseThrow(() -> new BizException("入库单不存在"));
        validateReceivable(order);
        return inboundView(order);
    }

    public ScanLocationView scanLocation(String locationCode) {
        StorageLocation location = locationRepository.findByCode(locationCode).orElseThrow(() -> new BizException("货位不存在或已禁用"));
        if (location.getStatus() == LocationStatus.DISABLED) throw new BizException("货位不存在或已禁用");
        return new ScanLocationView(location.getCode(), location.getWarehouse().getName(),
                location.getShelf() == null ? "" : location.getShelf().getCode(),
                location.getStatus(), location.getCapacity(), location.getOccupied());
    }

    @Transactional(readOnly = true)
    public ScanProductView scanInboundProduct(String orderNo, String productCode) {
        InboundOrder order = inboundOrderRepository.findByOrderNo(orderNo).orElseThrow(() -> new BizException("入库单不存在"));
        validateReceivable(order);
        InboundOrderItem item = findInboundItemByCode(order, productCode);
        Product product = item.getProduct();
        int expected = n(item.getQuantity());
        int received = n(item.getReceivedQuantity());
        return new ScanProductView(product.getSku(), product.getBarcode(), product.getName(), product.getModelSpec(),
                product.getUnitName(), expected, received, expected - received);
    }

    @Transactional
    public InboundOrderView receiveInbound(ReceiveRequest request) {
        if (request.quantity() == null || request.quantity() <= 0) throw new BizException("数量必须大于0");
        InboundOrder order = inboundOrderRepository.findByOrderNo(request.orderNo()).orElseThrow(() -> new BizException("入库单不存在"));
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.COMPLETED) {
            throw new BizException("入库单状态不允许收货");
        }
        StorageLocation location = locationRepository.findByCode(request.locationCode()).orElseThrow(() -> new BizException("货位不存在或已禁用"));
        if (location.getStatus() == LocationStatus.DISABLED) throw new BizException("货位不存在或已禁用");
        InboundOrderItem item = findInboundItemByCode(order, request.productCode());
        Product product = item.getProduct();
        int expected = n(item.getQuantity());
        int afterReceived = n(item.getReceivedQuantity()) + request.quantity();
        if (!Boolean.TRUE.equals(request.allowOverReceive()) && afterReceived > expected) {
            throw new BizException("数量不能超过剩余数量");
        }
        adjustStock(product, location.getWarehouse(), location, request.quantity(), MovementType.INBOUND,
                order.getOrderNo(), request.operatorName());
        item.setReceivedQuantity(afterReceived);
        boolean allDone = order.getItems().stream().allMatch(i -> n(i.getReceivedQuantity()) >= n(i.getQuantity()));
        if (allDone) {
            order.setStatus(OrderStatus.COMPLETED);
            order.setCompletedAt(LocalDateTime.now());
        }
        return inboundView(inboundOrderRepository.save(order));
    }

    private void adjustStock(Product product, Warehouse warehouse, StorageLocation location, int delta,
                             MovementType movementType, String sourceNo, String operator) {
        if (!warehouse.getId().equals(location.getWarehouse().getId())) throw new BizException("货位不属于所选仓库");
        if (location.getStatus() == LocationStatus.DISABLED) throw new BizException("货位已禁用");
        Stock stock = findOrCreateStock(product, warehouse, location);
        int before = stock.getQuantity();
        int after = before + delta;
        if (after < 0) throw new BizException("库存不足，无法出库");
        int occupiedAfter = n(location.getOccupied()) + delta;
        if (occupiedAfter < 0) throw new BizException("货位占用不能小于0");
        if (location.getCapacity() != null && location.getCapacity() > 0 && occupiedAfter > location.getCapacity()) {
            throw new BizException("货位容量不足");
        }
        stock.setQuantity(after);
        stockRepository.save(stock);
        location.setOccupied(occupiedAfter);
        location.setStatus(location.getCapacity() != null && location.getCapacity() > 0 && occupiedAfter >= location.getCapacity()
                ? LocationStatus.FULL : LocationStatus.ENABLED);
        locationRepository.save(location);
        product.setCurrentStock(n(product.getCurrentStock()) + delta);
        if (product.getCurrentStock() < 0) throw new BizException("商品总库存不能为负数");
        productRepository.save(product);
        movementRepository.save(movement(product, warehouse, location, movementType, Math.abs(delta), before, after, sourceNo, operator));
        refreshAlert(product);
    }

    private void validateLocationWarehouse(StorageLocation location, Warehouse warehouse) {
        if (!warehouse.getId().equals(location.getWarehouse().getId())) {
            throw new BizException("Location does not belong to selected warehouse");
        }
        if (location.getStatus() == LocationStatus.DISABLED) {
            throw new BizException("Location is disabled");
        }
    }

    private Stock findOrCreateStock(Product product, Warehouse warehouse, StorageLocation location) {
        return stockRepository.findByProductIdAndWarehouseIdAndLocationId(product.getId(), warehouse.getId(), location.getId()).orElseGet(() -> {
            Stock stock = new Stock();
            stock.setProduct(product);
            stock.setWarehouse(warehouse);
            stock.setLocation(location);
            stock.setQuantity(0);
            return stock;
        });
    }

    private StockMovement movement(Product product, Warehouse warehouse, StorageLocation location, MovementType type,
                                   int qty, int before, int after, String sourceNo, String operator) {
        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setWarehouse(warehouse);
        movement.setLocation(location);
        movement.setType(type);
        movement.setQuantity(qty);
        movement.setBeforeQuantity(before);
        movement.setAfterQuantity(after);
        movement.setSourceNo(sourceNo);
        movement.setOperatorName(operator);
        movement.setMovementTime(LocalDateTime.now());
        return movement;
    }

    private void refreshAlert(Product product) {
        try {
            if (n(product.getCurrentStock()) < n(product.getSafetyStock())) {
                alertRepository.findFirstByProductIdAndStatusNot(product.getId(), AlertStatus.RESOLVED).orElseGet(() -> {
                    StockAlert alert = new StockAlert();
                    alert.setProduct(product);
                    alert.setMessage("当前库存 " + product.getCurrentStock() + " 低于安全库存 " + product.getSafetyStock());
                    return alertRepository.save(alert);
                });
            }
        } catch (RuntimeException ignored) {
            // Alerts should not block stock operations.
        }
    }

    private StockStatus status(int quantity, int safetyStock) {
        if (quantity < safetyStock) return StockStatus.LOW;
        if (safetyStock > 0 && quantity > safetyStock * 3) return StockStatus.OVERSTOCK;
        return StockStatus.NORMAL;
    }

    private Product product(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new BizException("商品不存在"));
    }

    private Product findProductByCode(String code) {
        return productRepository.findBySku(code).or(() -> productRepository.findByBarcode(code))
                .orElseThrow(() -> new BizException("商品不存在"));
    }

    private InboundOrderItem findInboundItem(InboundOrder order, Product product) {
        return order.getItems().stream()
                .filter(i -> Objects.equals(i.getProduct().getId(), product.getId()))
                .findFirst()
                .orElseThrow(() -> new BizException("商品不在当前入库单中"));
    }

    private InboundOrderItem findInboundItemByCode(InboundOrder order, String code) {
        return order.getItems().stream()
                .filter(i -> Objects.equals(effectiveTrackingNo(i), code))
                .findFirst()
                .orElseGet(() -> findInboundItem(order, findProductByCode(code)));
    }

    private void validateReceivable(InboundOrder order) {
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new BizException("入库单不是待收货状态");
        }
    }

    private InboundOrderView inboundView(InboundOrder order) {
        List<InboundItemView> items = order.getItems().stream().map(item -> {
            int expected = n(item.getQuantity());
            int received = n(item.getReceivedQuantity());
            return inboundItemView(item, expected, received);
        }).toList();
        int expectedTotal = items.stream().mapToInt(InboundItemView::expectedQuantity).sum();
        int receivedTotal = items.stream().mapToInt(InboundItemView::receivedQuantity).sum();
        int progress = expectedTotal == 0 ? 0 : Math.min(100, receivedTotal * 100 / expectedTotal);
        return new InboundOrderView(order.getOrderNo(), order.getOperatorName(), order.getType().name(),
                order.getStatus(), expectedTotal, receivedTotal, progress, items);
    }

    private InboundItemView inboundItemView(InboundOrderItem item, int expected, int received) {
        Product p = item.getProduct();
        return new InboundItemView(item.getId(), p.getSku(), p.getBarcode(), p.getName(), p.getModelSpec(),
                p.getUnitName(), expected, received, expected - received, receiveStatus(expected, received),
                effectiveTrackingNo(item), item.getWarehouse().getName(), item.getLocation().getCode());
    }

    private InboundOrderDetailView inboundDetail(InboundOrder order) {
        InboundOrderView view = inboundView(order);
        List<OrderHistoryView> histories = new java.util.ArrayList<>();
        histories.add(new OrderHistoryView(order.getCreatedAt(), "CREATE", order.getOperatorName(), "创建入库订单"));
        movementRepository.findBySourceNoOrderByMovementTimeDesc(order.getOrderNo()).forEach(m ->
                histories.add(new OrderHistoryView(m.getMovementTime(), m.getType().name(), m.getOperatorName(),
                        m.getProduct().getSku() + " / " + m.getLocation().getCode() + " / " + m.getQuantity())));
        if (order.getCompletedAt() != null) {
            histories.add(new OrderHistoryView(order.getCompletedAt(), "COMPLETE", order.getOperatorName(), "入库订单完成"));
        }
        return new InboundOrderDetailView(order.getOrderNo(), order.getOperatorName(), order.getType().name(),
                order.getStatus(), order.getRemark(), order.getCreatedAt(), order.getCompletedAt(),
                view.expectedTotal(), view.receivedTotal(), view.progress(), view.items(), histories);
    }

    private String trackingNo(String orderNo, int index) {
        return orderNo + "-T" + String.format("%03d", index);
    }

    private String effectiveTrackingNo(InboundOrderItem item) {
        return item.getTrackingNo() == null
                ? trackingNo(item.getOrder().getOrderNo(), item.getId() == null ? 0 : item.getId().intValue())
                : item.getTrackingNo();
    }

    private OrderSummaryView inboundSummary(InboundOrder order) {
        return new OrderSummaryView(order.getId(), order.getOrderNo(), order.getType().name(), order.getStatus(),
                order.getOperatorName(), order.getRemark(), order.getItems().size());
    }

    private OrderSummaryView outboundSummary(OutboundOrder order) {
        return new OrderSummaryView(order.getId(), order.getOrderNo(), order.getType().name(), order.getStatus(),
                order.getOperatorName(), order.getRemark(), order.getItems().size());
    }

    private String receiveStatus(int expected, int received) {
        if (received <= 0) return "NOT_RECEIVED";
        if (received < expected) return "PARTIAL";
        if (received == expected) return "DONE";
        return "OVER";
    }

    private int n(Integer value) {
        return value == null ? 0 : value;
    }

    private Warehouse warehouse(Long id) {
        return warehouseRepository.findById(id).orElseThrow(() -> new BizException("仓库不存在"));
    }

    private StorageLocation location(Long id) {
        return locationRepository.findById(id).orElseThrow(() -> new BizException("货位不存在"));
    }
}
