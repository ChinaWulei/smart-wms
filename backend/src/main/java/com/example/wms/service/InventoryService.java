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
import com.example.wms.dto.OrderDtos.PickingLine;
import com.example.wms.dto.OrderDtos.PickingRequest;
import com.example.wms.dto.ViewDtos.StockView;
import com.example.wms.dto.WmsDtos.InboundItemView;
import com.example.wms.dto.WmsDtos.InboundOrderDetailView;
import com.example.wms.dto.WmsDtos.InboundOrderView;
import com.example.wms.dto.WmsDtos.OrderHistoryView;
import com.example.wms.dto.WmsDtos.OrderQ10mMetricView;
import com.example.wms.dto.WmsDtos.OrderSearchView;
import com.example.wms.dto.WmsDtos.OrderSummaryView;
import com.example.wms.dto.WmsDtos.OutboundItemView;
import com.example.wms.dto.WmsDtos.OutboundOrderDetailView;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
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
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${warehouse.realtime.clickhouse-url:}")
    private String clickHouseUrl;
    @Value("${warehouse.realtime.clickhouse-database:smart_wms_dw}")
    private String clickHouseDatabase;
    @Value("${warehouse.realtime.clickhouse-user:wms_dw}")
    private String clickHouseUser;
    @Value("${warehouse.realtime.clickhouse-password:}")
    private String clickHousePassword;

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
        order.setStatus(OrderStatus.IN_QUEUE);
        return inboundSummary(inboundOrderRepository.save(order));
    }

    @Transactional
    public OrderSummaryView outbound(OutboundRequest request) {
        OutboundOrder order = new OutboundOrder();
        order.setOrderNo("OUT" + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now()));
        order.setType(request.type());
        order.setOperatorName(request.operatorName());
        order.setReceiverName(request.receiverName());
        order.setReceiverPhone(request.receiverPhone());
        order.setAddress(request.address());
        order.setReason(request.reason());
        order.setTrackingNo(request.trackingNo());
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
            item.setAllocatedQuantity(0);
            item.setPickedQuantity(0);
            order.getItems().add(item);
        }
        order.setStatus(OrderStatus.IN_QUEUE);
        return outboundSummary(outboundOrderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryView> inboundOrders(Long warehouseId) {
        return inboundOrderRepository.findAll().stream()
                .filter(order -> warehouseId == null || order.getItems().stream()
                        .anyMatch(item -> item.getWarehouse().getId().equals(warehouseId)))
                .map(this::inboundSummary).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryView> outboundOrders(Long warehouseId) {
        return outboundOrderRepository.findAll().stream()
                .filter(order -> warehouseId == null || order.getItems().stream()
                        .anyMatch(item -> item.getWarehouse().getId().equals(warehouseId)))
                .map(this::outboundSummary).toList();
    }

    @Transactional(readOnly = true)
    public OrderQ10mMetricView orderQ10mCount() {
        if (blank(clickHouseUrl)) {
            throw new BizException("ClickHouse data warehouse is not configured");
        }
        try {
            String adsTable = clickHouseDatabase + ".ads_order_q_10m_timeout_summary";
            String query = String.format("""
                    select metric_code, metric_name, toString(metric_value), toString(update_time)
                    from %s
                    where metric_code = 'CONTINUOUS_Q_ORDER_10M'
                    order by update_time desc
                    limit 1
                    format TabSeparated
                    """, adsTable);
            HttpRequest request = HttpRequest.newBuilder(clickHouseUri())
                    .header("Content-Type", "text/plain; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(query, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 300) {
                throw new BizException("Failed to query ClickHouse ADS metric: " + compact(response.body()));
            }
            String line = response.body().lines().filter(item -> !item.isBlank()).findFirst().orElse("");
            if (line.isBlank()) {
                return new OrderQ10mMetricView("CONTINUOUS_Q_ORDER_10M",
                        "连续10分钟状态为Q的订单数量", 0, LocalDateTime.now());
            }
            String[] columns = line.split("\\t", -1);
            if (columns.length < 4) throw new BizException("Invalid ClickHouse ADS metric result");
            return new OrderQ10mMetricView(columns[0], columns[1], Integer.parseInt(columns[2]),
                    parseClickHouseTime(columns[3]));
        } catch (RuntimeException | java.io.IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            if (e instanceof BizException) throw (BizException) e;
            throw new BizException("Failed to query ClickHouse ADS metric: " + e.getMessage());
        }
    }

    private String compact(String value) {
        if (value == null || value.isBlank()) return "empty response";
        String compact = value.replaceAll("\\s+", " ").trim();
        return compact.length() > 300 ? compact.substring(0, 300) + "..." : compact;
    }

    private LocalDateTime parseClickHouseTime(String value) {
        try {
            return LocalDateTime.parse(value.replace(' ', 'T'));
        } catch (RuntimeException ignored) {
            return LocalDateTime.now();
        }
    }

    private URI clickHouseUri() {
        String separator = clickHouseUrl.contains("?") ? "&" : "?";
        String credentials = "database=" + encode(clickHouseDatabase)
                + "&user=" + encode(clickHouseUser);
        if (!blank(clickHousePassword)) {
            credentials += "&password=" + encode(clickHousePassword);
        }
        return URI.create(clickHouseUrl + separator + credentials);
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public OutboundOrderDetailView getOutboundOrder(Long id) {
        return outboundDetail(outboundOrderRepository.findById(id)
                .orElseThrow(() -> new BizException("出库单不存在")));
    }

    @Transactional
    public OutboundOrderDetailView generatePickList(Long id, String operatorName) {
        OutboundOrder order = outboundOrderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BizException("Outbound order does not exist"));
        if (!isInQueue(order.getStatus())) {
            throw new BizException("Only In Queue outbound orders can generate a pick list");
        }
        Map<OutboundStockKey, Stock> lockedStocks = new LinkedHashMap<>();
        for (OutboundStockKey key : requiredStock(order).keySet()) {
            stockRepository.findForUpdate(key.productId(), key.warehouseId(), key.locationId())
                    .ifPresent(stock -> lockedStocks.put(key, stock));
        }
        Map<OutboundStockKey, Integer> remainingAvailable = new LinkedHashMap<>();
        lockedStocks.forEach((key, stock) -> remainingAvailable.put(
                key, Math.max(0, n(stock.getQuantity()) - n(stock.getAllocatedQuantity()))));
        List<String> shortages = new java.util.ArrayList<>();
        for (OutboundOrderItem item : order.getItems()) {
            OutboundStockKey key = stockKey(item);
            int required = n(item.getQuantity());
            int allocated = Math.min(required, remainingAvailable.getOrDefault(key, 0));
            item.setAllocatedQuantity(allocated);
            remainingAvailable.put(key, remainingAvailable.getOrDefault(key, 0) - allocated);
            if (allocated < required) {
                shortages.add(item.getProduct().getSku() + " " + item.getProduct().getName()
                        + ": shortage " + (required - allocated) + " item(s)");
            }
        }
        lockedStocks.forEach((key, stock) -> {
            int allocated = order.getItems().stream()
                    .filter(item -> stockKey(item).equals(key))
                    .mapToInt(item -> n(item.getAllocatedQuantity()))
                    .sum();
            stock.setAllocatedQuantity(n(stock.getAllocatedQuantity()) + allocated);
            stockRepository.save(stock);
        });
        order.setAllocatedAt(LocalDateTime.now());
        order.setAllocatedBy(operatorName);
        if (shortages.isEmpty()) {
            order.setStatus(OrderStatus.ALLOCATED);
            order.setShortageAt(null);
            order.setShortageBy(null);
            order.setShortageDetails(null);
        } else {
            order.setStatus(OrderStatus.NOT_ENOUGH_INV);
            order.setShortageAt(LocalDateTime.now());
            order.setShortageBy(operatorName);
            order.setShortageDetails(String.join("; ", shortages));
        }
        return outboundDetail(outboundOrderRepository.save(order));
    }

    @Transactional
    public OutboundOrderDetailView assignPicking(Long id, String operatorName, boolean continueOnShortage) {
        OutboundOrder order = outboundOrderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BizException("Outbound order does not exist"));
        if (order.getStatus() == OrderStatus.NOT_ENOUGH_INV) {
            if (!continueOnShortage) {
                throw new BizException("Inventory is insufficient. Confirm whether to create a back outbound order and continue.");
            }
            if (!splitShortageToBackOrder(order, operatorName)) {
                order.setStatus(OrderStatus.CANCELLED);
                order.setCancelledAt(LocalDateTime.now());
                order.setCancelledBy(operatorName);
                order.setAssignedAt(LocalDateTime.now());
                order.setAssignedBy(operatorName);
                return outboundDetail(outboundOrderRepository.save(order));
            }
        } else if (order.getStatus() != OrderStatus.ALLOCATED) {
            throw new BizException("Only Allocated or Not Enough Inv outbound orders can be assigned for picking");
        }
        order.setStatus(OrderStatus.READY_TO_PICK);
        order.setAssignedAt(LocalDateTime.now());
        order.setAssignedBy(operatorName);
        return outboundDetail(outboundOrderRepository.save(order));
    }

    private boolean splitShortageToBackOrder(OutboundOrder order, String operatorName) {
        OutboundOrder backOrder = copyBackOrderHeader(order, operatorName);
        List<OutboundOrderItem> currentItems = new java.util.ArrayList<>(order.getItems());
        for (OutboundOrderItem item : currentItems) {
            int shortage = n(item.getQuantity()) - n(item.getAllocatedQuantity());
            if (shortage > 0) {
                backOrder.getItems().add(copyBackOrderItem(backOrder, item, shortage));
            }
            if (n(item.getAllocatedQuantity()) <= 0) {
                order.getItems().remove(item);
            } else {
                item.setQuantity(n(item.getAllocatedQuantity()));
            }
        }
        OutboundOrder savedBackOrder = outboundOrderRepository.save(backOrder);
        order.setBackOrderNo(savedBackOrder.getOrderNo());
        return !order.getItems().isEmpty();
    }

    private OutboundOrder copyBackOrderHeader(OutboundOrder source, String operatorName) {
        OutboundOrder backOrder = new OutboundOrder();
        backOrder.setOrderNo("BACKOUT" + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now()));
        backOrder.setType(source.getType());
        backOrder.setStatus(OrderStatus.IN_QUEUE);
        backOrder.setOperatorName(operatorName);
        backOrder.setReceiverName(source.getReceiverName());
        backOrder.setReceiverPhone(source.getReceiverPhone());
        backOrder.setAddress(source.getAddress());
        backOrder.setReason(source.getReason());
        backOrder.setTrackingNo(source.getTrackingNo());
        backOrder.setRemark("Back outbound order for " + source.getOrderNo());
        return backOrder;
    }

    private OutboundOrderItem copyBackOrderItem(OutboundOrder backOrder, OutboundOrderItem source, int quantity) {
        OutboundOrderItem item = new OutboundOrderItem();
        item.setOrder(backOrder);
        item.setProduct(source.getProduct());
        item.setWarehouse(source.getWarehouse());
        item.setLocation(source.getLocation());
        item.setQuantity(quantity);
        item.setAllocatedQuantity(0);
        item.setPickedQuantity(0);
        return item;
    }

    @Transactional
    public OutboundOrderDetailView startPicking(Long id, String operatorName) {
        OutboundOrder order = outboundOrderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BizException("出库单不存在"));
        if (order.getStatus() != OrderStatus.READY_TO_PICK) {
            throw new BizException("只有待拣货的出库单可以开始拣货");
        }
        order.setStatus(OrderStatus.PICKING);
        order.setPickingStartedAt(LocalDateTime.now());
        order.setPickingStartedBy(operatorName);
        return outboundDetail(outboundOrderRepository.save(order));
    }

    @Transactional
    public OutboundOrderDetailView completePicking(Long id, PickingRequest request) {
        OutboundOrder order = outboundOrderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BizException("出库单不存在"));
        if (order.getStatus() != OrderStatus.PICKING) {
            throw new BizException("出库单不是拣货中状态");
        }
        Map<Long, Integer> pickedByItem = request.items().stream()
                .collect(java.util.stream.Collectors.toMap(PickingLine::itemId, PickingLine::pickedQuantity,
                        (first, second) -> second));
        for (OutboundOrderItem item : order.getItems()) {
            Integer picked = pickedByItem.get(item.getId());
            if (picked == null || picked != n(item.getQuantity())) {
                throw new BizException("拣货数量必须与订单数量一致");
            }
            item.setPickedQuantity(picked);
        }
        order.setStatus(OrderStatus.PICKED);
        order.setPickedAt(LocalDateTime.now());
        order.setPickedBy(request.operatorName());
        return outboundDetail(outboundOrderRepository.save(order));
    }

    @Transactional
    public OutboundOrderDetailView confirmOutbound(Long id, String operatorName) {
        OutboundOrder order = outboundOrderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BizException("出库单不存在"));
        if (order.getStatus() != OrderStatus.PICKED && order.getStatus() != OrderStatus.PACKED) {
            if (order.getStatus() == OrderStatus.COMPLETED) throw new BizException("出库单已完成，不能重复确认");
            if (order.getStatus() == OrderStatus.CANCELLED) throw new BizException("出库单已取消，不能确认");
            throw new BizException("请先完成拣货/打包，再确认出库");
        }
        for (OutboundOrderItem item : order.getItems()) {
            Stock stock = stockRepository.findForUpdate(item.getProduct().getId(), item.getWarehouse().getId(),
                            item.getLocation().getId())
                    .orElseThrow(() -> new BizException("Current inventory is insufficient for outbound"));
            if (n(stock.getQuantity()) < n(item.getPickedQuantity())
                    || n(stock.getAllocatedQuantity()) < n(item.getAllocatedQuantity())) {
                throw new BizException("Current inventory is insufficient for outbound");
            }
            stock.setAllocatedQuantity(n(stock.getAllocatedQuantity()) - n(item.getAllocatedQuantity()));
            stockRepository.save(stock);
            adjustStock(item.getProduct(), item.getWarehouse(), item.getLocation(), -n(item.getPickedQuantity()),
                    MovementType.OUTBOUND, order.getOrderNo(), operatorName);
            item.setAllocatedQuantity(0);
        }
        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        order.setCompletedBy(operatorName);
        return outboundDetail(outboundOrderRepository.save(order));
    }

    private Map<OutboundStockKey, Integer> requiredStock(OutboundOrder order) {
        Map<OutboundStockKey, Integer> requiredByStock = new LinkedHashMap<>();
        order.getItems().stream()
                .sorted(Comparator.comparing((OutboundOrderItem item) -> item.getProduct().getId())
                        .thenComparing(item -> item.getLocation().getId()))
                .forEach(item -> requiredByStock.merge(new OutboundStockKey(
                        item.getProduct().getId(), item.getWarehouse().getId(), item.getLocation().getId()),
                        n(item.getQuantity()), Integer::sum));
        return requiredByStock;
    }

    private OutboundStockKey stockKey(OutboundOrderItem item) {
        return new OutboundStockKey(item.getProduct().getId(), item.getWarehouse().getId(), item.getLocation().getId());
    }

    @Transactional
    public OutboundOrderDetailView cancelOutbound(Long id, String operatorName) {
        OutboundOrder current = outboundOrderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BizException("Outbound order does not exist"));
        if (!isInQueue(current.getStatus())) {
            throw new BizException("Only In Queue outbound orders can be cancelled");
        }
        OutboundOrder order = outboundOrderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BizException("出库单不存在"));
        if (order.getStatus() == OrderStatus.COMPLETED) throw new BizException("已完成的出库单不能取消");
        if (order.getStatus() == OrderStatus.CANCELLED) throw new BizException("出库单已取消");
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelledBy(operatorName);
        return outboundDetail(outboundOrderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderSearchView> searchOrders(String orderNo, String direction, OrderStatus status,
                                              LocalDate createdFrom, LocalDate createdTo, String operatorName,
                                              Long warehouseId) {
        LocalDateTime from = createdFrom == null ? null : createdFrom.atStartOfDay();
        LocalDateTime to = createdTo == null ? null : createdTo.plusDays(1).atStartOfDay();
        Stream<OrderSearchView> orders = Stream.concat(
                inboundOrderRepository.findAll().stream()
                        .filter(order -> warehouseId == null || order.getItems().stream()
                                .anyMatch(item -> item.getWarehouse().getId().equals(warehouseId)))
                        .map(this::inboundSearchView),
                outboundOrderRepository.findAll().stream()
                        .filter(order -> warehouseId == null || order.getItems().stream()
                                .anyMatch(item -> item.getWarehouse().getId().equals(warehouseId)))
                        .map(this::outboundSearchView));
        return orders
                .filter(order -> blank(direction) || order.direction().equalsIgnoreCase(direction))
                .filter(order -> blank(orderNo) || containsIgnoreCase(order.orderNo(), orderNo))
                .filter(order -> status == null || order.status() == status)
                .filter(order -> from == null || !order.createdAt().isBefore(from))
                .filter(order -> to == null || order.createdAt().isBefore(to))
                .filter(order -> blank(operatorName) || containsIgnoreCase(order.operatorName(), operatorName))
                .sorted(Comparator.comparing(OrderSearchView::createdAt).reversed())
                .toList();
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
                        s.getLocation().getCode(), s.getQuantity(), n(s.getAllocatedQuantity()),
                        n(s.getQuantity()) - n(s.getAllocatedQuantity()), s.getProduct().getSafetyStock(),
                        status(s.getQuantity(), s.getProduct().getSafetyStock())))
                .toList();
    }

    @Transactional(readOnly = true)
    public InboundOrderDetailView getInboundOrder(String orderNo) {
        return inboundDetail(inboundOrderRepository.findByOrderNo(orderNo).orElseThrow(() -> new BizException("入库单不存在")));
    }

    @Transactional
    public InboundOrderView getReceivableInboundOrder(String orderNo) {
        InboundOrder order = inboundOrderRepository.findByOrderNo(orderNo).orElseThrow(() -> new BizException("入库单不存在"));
        validateReceivable(order);
        if (isInQueue(order.getStatus())) {
            order.setStatus(OrderStatus.RECEIVING);
            order.setReceivingStartedAt(LocalDateTime.now());
            inboundOrderRepository.save(order);
        }
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
        if (order.getStatus() != OrderStatus.RECEIVING) {
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
            order.setStatus(OrderStatus.RECEIVED);
            order.setReceivedAt(LocalDateTime.now());
        }
        return inboundView(inboundOrderRepository.save(order));
    }

    @Transactional
    public InboundOrderDetailView confirmInbound(String orderNo, String operatorName) {
        InboundOrder order = inboundOrderRepository.findByOrderNoForUpdate(orderNo)
                .orElseThrow(() -> new BizException("Inbound order does not exist"));
        if (order.getStatus() != OrderStatus.RECEIVED) {
            throw new BizException("Only Received inbound orders can be confirmed");
        }
        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        order.setCompletedBy(operatorName);
        return inboundDetail(inboundOrderRepository.save(order));
    }

    @Transactional
    public InboundOrderDetailView cancelInbound(String orderNo, String operatorName) {
        InboundOrder order = inboundOrderRepository.findByOrderNoForUpdate(orderNo)
                .orElseThrow(() -> new BizException("Inbound order does not exist"));
        if (!isInQueue(order.getStatus())) {
            throw new BizException("Only In Queue inbound orders can be cancelled");
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelledBy(operatorName);
        return inboundDetail(inboundOrderRepository.save(order));
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
            stock.setAllocatedQuantity(0);
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
        movement.setRemark(type == MovementType.INBOUND ? "收货入库"
                : type == MovementType.OUTBOUND ? "确认出库" : "库存调整");
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
        if (!isInQueue(order.getStatus()) && order.getStatus() != OrderStatus.RECEIVING) {
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
                order.getStatus(), order.getRemark(), order.getCreatedAt(), order.getReceivingStartedAt(),
                order.getReceivedAt(), order.getCompletedAt(), order.getCancelledAt(),
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

    private OutboundOrderDetailView outboundDetail(OutboundOrder order) {
        List<OutboundItemView> items = order.getItems().stream().map(item -> {
            Product product = item.getProduct();
            Stock stock = stockRepository.findByProductIdAndWarehouseIdAndLocationId(
                    product.getId(), item.getWarehouse().getId(), item.getLocation().getId())
                    .orElse(null);
            int available = stock == null ? 0 : n(stock.getQuantity()) - n(stock.getAllocatedQuantity());
            String pickingStatus = n(item.getPickedQuantity()) <= 0 ? "PENDING"
                    : n(item.getPickedQuantity()) < n(item.getQuantity()) ? "PARTIAL" : "PICKED";
            return new OutboundItemView(item.getId(), product.getId(), product.getSku(), product.getBarcode(),
                    product.getName(), product.getModelSpec(), product.getUnitName(), item.getQuantity(),
                    n(item.getAllocatedQuantity()), n(item.getPickedQuantity()), pickingStatus,
                    item.getWarehouse().getId(), item.getWarehouse().getName(), item.getLocation().getId(),
                    item.getLocation().getCode(),
                    item.getLocation().getShelf() == null ? "" : item.getLocation().getShelf().getCode(), available);
        }).toList();
        List<OrderHistoryView> histories = new java.util.ArrayList<>();
        histories.add(new OrderHistoryView(order.getCreatedAt(), "CREATE", order.getOperatorName(), "创建出库订单"));
        if (order.getShortageAt() != null) {
            histories.add(new OrderHistoryView(order.getShortageAt(), "SHORTAGE",
                    order.getShortageBy(), order.getShortageDetails()));
        }
        if (order.getBackOrderNo() != null) {
            histories.add(new OrderHistoryView(order.getAssignedAt(), "BACK_ORDER",
                    order.getAssignedBy(), "Created back outbound order " + order.getBackOrderNo()));
        }
        if (order.getPickingStartedAt() != null) {
            histories.add(new OrderHistoryView(order.getPickingStartedAt(), "PICKING",
                    order.getPickingStartedBy(), "开始拣货"));
        }
        if (order.getPickedAt() != null) {
            histories.add(new OrderHistoryView(order.getPickedAt(), "PICKED", order.getPickedBy(), "拣货完成"));
        }
        movementRepository.findBySourceNoOrderByMovementTimeDesc(order.getOrderNo()).forEach(movement ->
                histories.add(new OrderHistoryView(movement.getMovementTime(), movement.getType().name(),
                        movement.getOperatorName(), movement.getProduct().getSku() + " / "
                        + movement.getLocation().getCode() + " / " + movement.getQuantity())));
        if (order.getCompletedAt() != null) {
            histories.add(new OrderHistoryView(order.getCompletedAt(), "COMPLETE", order.getCompletedBy(), "出库订单完成"));
        }
        if (order.getCancelledAt() != null) {
            histories.add(new OrderHistoryView(order.getCancelledAt(), "CANCEL", order.getCancelledBy(), "取消出库订单"));
        }
        return new OutboundOrderDetailView(order.getId(), order.getOrderNo(), order.getType().name(),
                order.getStatus(), order.getOperatorName(), order.getReceiverName(), order.getReceiverPhone(),
                order.getAddress(), order.getReason(), order.getTrackingNo(), order.getRemark(),
                order.getShortageDetails(), order.getBackOrderNo(),
                order.getCreatedAt(), order.getAllocatedAt(), order.getAssignedAt(),
                order.getPickingStartedAt(), order.getPickedAt(),
                order.getCompletedAt(), order.getCancelledAt(),
                items.stream().mapToInt(item -> n(item.quantity())).sum(), items, histories);
    }

    private OrderSearchView inboundSearchView(InboundOrder order) {
        return new OrderSearchView(order.getId(), order.getOrderNo(), "INBOUND", order.getType().name(),
                order.getStatus(), order.getOperatorName(), order.getRemark(), order.getItems().size(),
                order.getCreatedAt(), order.getCompletedAt());
    }

    private OrderSearchView outboundSearchView(OutboundOrder order) {
        return new OrderSearchView(order.getId(), order.getOrderNo(), "OUTBOUND", order.getType().name(),
                order.getStatus(), order.getOperatorName(), order.getRemark(), order.getItems().size(),
                order.getCreatedAt(), order.getCompletedAt());
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword.trim().toLowerCase());
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

    private boolean isInQueue(OrderStatus status) {
        return status == OrderStatus.IN_QUEUE || status == OrderStatus.CREATED;
    }

    private boolean isQStatus(OrderStatus status) {
        return isInQueue(status);
    }

    private record OutboundStockKey(Long productId, Long warehouseId, Long locationId) {}

    private Warehouse warehouse(Long id) {
        return warehouseRepository.findById(id).orElseThrow(() -> new BizException("仓库不存在"));
    }

    private StorageLocation location(Long id) {
        return locationRepository.findById(id).orElseThrow(() -> new BizException("货位不存在"));
    }
}
