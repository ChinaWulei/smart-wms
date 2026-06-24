package com.example.wms.shipping;

import com.example.wms.common.BizException;
import com.example.wms.domain.OutboundOrder;
import com.example.wms.domain.OutboundOrderItem;
import com.example.wms.domain.enums.OrderStatus;
import com.example.wms.repository.OutboundOrderRepository;
import com.example.wms.repository.WarehouseRepository;
import com.example.wms.service.InventoryService;
import com.example.wms.shipping.ShippingJob.ShippingOrderRef;
import com.example.wms.shipping.ShippingJobDtos.CreateShippingJobRequest;
import com.example.wms.shipping.ShippingJobDtos.UpdateShippingJobRequest;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ShippingJobService {
    private final ShippingJobRepository shippingJobRepository;
    private final ShippingOrderRepository shippingOrderRepository;
    private final OutboundOrderRepository outboundOrderRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryService inventoryService;

    public ShippingJobService(ShippingJobRepository shippingJobRepository,
                              ShippingOrderRepository shippingOrderRepository,
                              OutboundOrderRepository outboundOrderRepository,
                              WarehouseRepository warehouseRepository,
                              InventoryService inventoryService) {
        this.shippingJobRepository = shippingJobRepository;
        this.shippingOrderRepository = shippingOrderRepository;
        this.outboundOrderRepository = outboundOrderRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public ShippingJob create(CreateShippingJobRequest request) {
        var warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new BizException("Warehouse does not exist"));
        ShippingJob job = new ShippingJob();
        LocalDateTime now = LocalDateTime.now();
        job.setWarehouseId(warehouse.getId());
        job.setWarehouseCode(warehouse.getCode());
        job.setWarehouseCode3(warehouseCode3(warehouse.getCode()));
        job.setSequence(nextSequence(job.getWarehouseCode3()));
        job.setJobNo(nextJobNo(job.getWarehouseCode3(), job.getSequence()));
        job.setPlannedShipDate(request.plannedShipDate());
        job.setTruckNo(trim(request.truckNo()));
        job.setDriverName(trim(request.driverName()));
        job.setDriverPhone(trim(request.driverPhone()));
        job.setRemark(trim(request.remark()));
        job.setCreatedBy(trim(request.createdBy()));
        touchForCreate(job, now);
        attachOrders(job, request.outboundOrderIds());
        ShippingJob saved = shippingJobRepository.save(job);
        syncShippingOrders(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ShippingJob> list(Long warehouseId) {
        return warehouseId == null
                ? shippingJobRepository.findAllByOrderByCreatedAtDesc()
                : shippingJobRepository.findByWarehouseIdOrderByCreatedAtDesc(warehouseId);
    }

    @Transactional(readOnly = true)
    public ShippingJob get(String id) {
        return find(id);
    }

    @Transactional
    public ShippingJob update(String id, UpdateShippingJobRequest request) {
        ShippingJob job = editable(id);
        job.setPlannedShipDate(request.plannedShipDate());
        job.setTruckNo(trim(request.truckNo()));
        job.setDriverName(trim(request.driverName()));
        job.setDriverPhone(trim(request.driverPhone()));
        job.setRemark(trim(request.remark()));
        touchForUpdate(job);
        return shippingJobRepository.save(job);
    }

    @Transactional
    public ShippingJob addOrders(String id, Collection<Long> orderIds) {
        ShippingJob job = editable(id);
        attachOrders(job, orderIds);
        touchForUpdate(job);
        ShippingJob saved = shippingJobRepository.save(job);
        syncShippingOrders(saved);
        return saved;
    }

    @Transactional
    public ShippingJob removeOrder(String id, Long orderId) {
        ShippingJob job = editable(id);
        boolean removed = job.getOrders().removeIf(order -> orderId.equals(order.getOrderId()));
        if (!removed) throw new BizException("Outbound order is not bound to this shipping job");
        touchForUpdate(job);
        shippingOrderRepository.deleteByShippingJobIdAndOrderId(job.getId(), orderId);
        return shippingJobRepository.save(job);
    }

    @Transactional
    public ShippingJob startToShip(String id) {
        ShippingJob job = editable(id);
        if (job.getOrders().isEmpty()) throw new BizException("Shipping job must contain at least one outbound order");
        List<OutboundOrder> orders = outboundOrderRepository.findAllById(
                job.getOrders().stream().map(ShippingOrderRef::getOrderId).toList());
        if (orders.size() != job.getOrders().size()) throw new BizException("Some outbound orders no longer exist");
        List<String> notPacked = orders.stream()
                .filter(order -> !isPacked(order.getStatus()))
                .map(OutboundOrder::getOrderNo)
                .toList();
        if (!notPacked.isEmpty()) {
            throw new BizException("以下订单还未 Packed，不能 Start to Ship: " + String.join(", ", notPacked));
        }
        refreshOrderSnapshots(job, orders);
        job.setStatus(ShippingJobStatus.READY_TO_SORT);
        touchForUpdate(job);
        return shippingJobRepository.save(job);
    }

    @Transactional
    public ShippingJob cancel(String id) {
        ShippingJob job = find(id);
        if (job.getStatus() == ShippingJobStatus.COMPLETED) {
            throw new BizException("A completed job cannot be cancelled");
        }
        job.setStatus(ShippingJobStatus.CANCELLED);
        touchForUpdate(job);
        for (ShippingOrder order : shippingOrderRepository.findByShippingJobIdOrderByOrderNoAsc(job.getId())) {
            order.setStatus(ShippingOrderStatus.CANCELLED);
            order.setUpdatedAt(LocalDateTime.now());
            shippingOrderRepository.save(order);
        }
        return shippingJobRepository.save(job);
    }

    @Transactional
    public ShippingJob complete(String id, String operatorName) {
        ShippingJob job = find(id);
        if (job.getStatus() != ShippingJobStatus.SHIPPED) {
            throw new BizException("Only a Shipped shipping job can be completed");
        }
        List<ShippingOrder> shippingOrders = shippingOrderRepository.findByShippingJobIdOrderByOrderNoAsc(job.getId());
        if (shippingOrders.isEmpty()) throw new BizException("Shipping job has no shipping orders");
        boolean allDone = shippingOrders.stream().allMatch(order -> order.getStatus() == ShippingOrderStatus.DONE);
        if (!allDone) throw new BizException("还有货物未全部上车，不能 Completed");
        for (ShippingOrder shippingOrder : shippingOrders) {
            inventoryService.confirmOutbound(shippingOrder.getOrderId(), operatorName);
        }
        List<OutboundOrder> outboundOrders = outboundOrderRepository.findAllById(
                shippingOrders.stream().map(ShippingOrder::getOrderId).toList());
        refreshOrderSnapshots(job, outboundOrders);
        job.setStatus(ShippingJobStatus.COMPLETED);
        touchForUpdate(job);
        return shippingJobRepository.save(job);
    }

    private ShippingJob editable(String id) {
        ShippingJob job = find(id);
        if (job.getStatus() != ShippingJobStatus.IN_QUEUE && job.getStatus() != ShippingJobStatus.DRAFT) {
            throw new BizException("Only an In Queue shipping job can be edited");
        }
        return job;
    }

    private ShippingJob find(String id) {
        return shippingJobRepository.findById(id)
                .orElseThrow(() -> new BizException("Shipping job does not exist"));
    }

    private void attachOrders(ShippingJob job, Collection<Long> requestedIds) {
        if (requestedIds == null || requestedIds.isEmpty()) return;
        Set<Long> ids = new LinkedHashSet<>(requestedIds);
        Set<Long> existingIds = job.getOrders().stream().map(ShippingOrderRef::getOrderId)
                .collect(java.util.stream.Collectors.toSet());
        ids.removeAll(existingIds);
        if (ids.isEmpty()) return;
        List<OutboundOrder> orders = outboundOrderRepository.findAllById(ids);
        if (orders.size() != ids.size()) throw new BizException("Some outbound orders do not exist");
        for (OutboundOrder order : orders) {
            validateOrder(job, order);
            job.getOrders().add(snapshot(order));
        }
    }

    private void validateOrder(ShippingJob job, OutboundOrder order) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BizException("Cancelled outbound order cannot be bound: " + order.getOrderNo());
        }
        if (order.getItems().isEmpty()) {
            throw new BizException("Outbound order has no items: " + order.getOrderNo());
        }
        boolean wrongWarehouse = order.getItems().stream()
                .map(OutboundOrderItem::getWarehouse)
                .anyMatch(warehouse -> !job.getWarehouseId().equals(warehouse.getId()));
        if (wrongWarehouse) {
            throw new BizException("Outbound order belongs to another warehouse: " + order.getOrderNo());
        }
        shippingJobRepository.findFirstByOrdersOrderIdAndStatusNot(order.getId(), ShippingJobStatus.CANCELLED)
                .filter(existing -> !existing.getId().equals(job.getId()))
                .ifPresent(existing -> {
                    throw new BizException("Outbound order " + order.getOrderNo()
                            + " is already bound to " + existing.getJobNo());
                });
    }

    private ShippingOrderRef snapshot(OutboundOrder order) {
        ShippingOrderRef ref = new ShippingOrderRef();
        ref.setOrderId(order.getId());
        ref.setOrderNo(order.getOrderNo());
        ref.setReceiverName(order.getReceiverName());
        ref.setAddress(order.getAddress());
        ref.setOrderStatus(order.getStatus().name());
        ref.setItemCount(order.getItems().size());
        return ref;
    }

    private void refreshOrderSnapshots(ShippingJob job, List<OutboundOrder> orders) {
        var byId = orders.stream().collect(java.util.stream.Collectors.toMap(OutboundOrder::getId, order -> order));
        job.setOrders(job.getOrders().stream().map(ref -> snapshot(byId.get(ref.getOrderId()))).toList());
        syncShippingOrders(job, orders);
    }

    private Long nextSequence(String warehouseCode3) {
        return shippingJobRepository.findTopByWarehouseCode3OrderBySequenceDesc(warehouseCode3)
                .map(ShippingJob::getSequence)
                .filter(value -> value != null)
                .map(value -> value + 1)
                .orElse(1L);
    }

    private String nextJobNo(String warehouseCode3, Long sequence) {
        return "SH-" + warehouseCode3 + "-" + String.format("%08d", sequence);
    }

    private String warehouseCode3(String warehouseCode) {
        String normalized = (StringUtils.hasText(warehouseCode) ? warehouseCode : "WMS")
                .replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase(Locale.ROOT);
        if (normalized.length() >= 3) return normalized.substring(0, 3);
        return (normalized + "WMS").substring(0, 3);
    }

    private void touchForCreate(ShippingJob job, LocalDateTime now) {
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
    }

    private void touchForUpdate(ShippingJob job) {
        job.setUpdatedAt(LocalDateTime.now());
    }

    private void syncShippingOrders(ShippingJob job) {
        if (job.getId() == null || job.getOrders().isEmpty()) return;
        List<Long> orderIds = job.getOrders().stream().map(ShippingOrderRef::getOrderId).toList();
        syncShippingOrders(job, outboundOrderRepository.findAllById(orderIds));
    }

    private void syncShippingOrders(ShippingJob job, List<OutboundOrder> orders) {
        if (job.getId() == null) return;
        LocalDateTime now = LocalDateTime.now();
        Set<Long> currentIds = job.getOrders().stream().map(ShippingOrderRef::getOrderId)
                .collect(java.util.stream.Collectors.toSet());
        List<Long> storedIds = shippingOrderRepository.findByShippingJobIdOrderByOrderNoAsc(job.getId())
                .stream().map(ShippingOrder::getOrderId).toList();
        List<Long> removedIds = storedIds.stream().filter(id -> !currentIds.contains(id)).toList();
        if (!removedIds.isEmpty()) {
            shippingOrderRepository.deleteByShippingJobIdAndOrderIdIn(job.getId(), removedIds);
        }
        for (OutboundOrder outboundOrder : orders) {
            ShippingOrder shippingOrder = shippingOrderRepository
                    .findByShippingJobIdAndOrderId(job.getId(), outboundOrder.getId())
                    .orElseGet(ShippingOrder::new);
            if (shippingOrder.getCreatedAt() == null) shippingOrder.setCreatedAt(now);
            shippingOrder.setUpdatedAt(now);
            shippingOrder.setShippingJobId(job.getId());
            shippingOrder.setJobNo(job.getJobNo());
            shippingOrder.setOrderId(outboundOrder.getId());
            shippingOrder.setOrderNo(outboundOrder.getOrderNo());
            shippingOrder.setWarehouseId(job.getWarehouseId());
            shippingOrder.setReceiverName(outboundOrder.getReceiverName());
            shippingOrder.setReceiverPhone(outboundOrder.getReceiverPhone());
            shippingOrder.setAddress(outboundOrder.getAddress());
            if (shippingOrder.getStatus() == null || shippingOrder.getStatus() == ShippingOrderStatus.CANCELLED) {
                shippingOrder.setStatus(ShippingOrderStatus.PENDING);
            }
            Map<String, Integer> scannedBySku = new HashMap<>();
            for (ShippingOrder.Item item : shippingOrder.getItems()) {
                scannedBySku.put(item.getSku(), item.getScannedQuantity());
            }
            shippingOrder.setItems(outboundOrder.getItems().stream()
                    .map(item -> shippingItem(item, scannedBySku))
                    .toList());
            updateShippingOrderStatus(shippingOrder);
            shippingOrderRepository.save(shippingOrder);
        }
    }

    private ShippingOrder.Item shippingItem(OutboundOrderItem item, Map<String, Integer> scannedBySku) {
        ShippingOrder.Item result = new ShippingOrder.Item();
        result.setProductId(item.getProduct().getId());
        result.setSku(item.getProduct().getSku());
        result.setBarcode(item.getProduct().getBarcode());
        result.setProductName(item.getProduct().getName());
        result.setLocationCode(item.getLocation().getCode());
        result.setRequiredQuantity(item.getQuantity() == null ? 0 : item.getQuantity());
        result.setScannedQuantity(scannedBySku.getOrDefault(result.getSku(), 0));
        return result;
    }

    static void updateShippingOrderStatus(ShippingOrder order) {
        boolean anyScanned = order.getItems().stream()
                .anyMatch(item -> safe(item.getScannedQuantity()) > 0);
        boolean allDone = !order.getItems().isEmpty() && order.getItems().stream()
                .allMatch(item -> safe(item.getScannedQuantity()) >= safe(item.getRequiredQuantity()));
        if (allDone) {
            order.setStatus(ShippingOrderStatus.DONE);
            if (order.getCompletedAt() == null) order.setCompletedAt(LocalDateTime.now());
        } else {
            order.setStatus(anyScanned ? ShippingOrderStatus.PARTIAL : ShippingOrderStatus.PENDING);
            order.setCompletedAt(null);
        }
    }

    static int safe(Integer value) {
        return value == null ? 0 : value;
    }

    static boolean isPacked(OrderStatus status) {
        return status == OrderStatus.PACKED || status == OrderStatus.PICKED;
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
