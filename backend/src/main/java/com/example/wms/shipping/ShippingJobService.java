package com.example.wms.shipping;

import com.example.wms.common.BizException;
import com.example.wms.domain.OutboundOrder;
import com.example.wms.domain.OutboundOrderItem;
import com.example.wms.domain.enums.OrderStatus;
import com.example.wms.repository.OutboundOrderRepository;
import com.example.wms.repository.WarehouseRepository;
import com.example.wms.shipping.ShippingJob.ShippingOrderRef;
import com.example.wms.shipping.ShippingJobDtos.CreateShippingJobRequest;
import com.example.wms.shipping.ShippingJobDtos.UpdateShippingJobRequest;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ShippingJobService {
    private final ShippingJobRepository shippingJobRepository;
    private final OutboundOrderRepository outboundOrderRepository;
    private final WarehouseRepository warehouseRepository;

    public ShippingJobService(ShippingJobRepository shippingJobRepository,
                              OutboundOrderRepository outboundOrderRepository,
                              WarehouseRepository warehouseRepository) {
        this.shippingJobRepository = shippingJobRepository;
        this.outboundOrderRepository = outboundOrderRepository;
        this.warehouseRepository = warehouseRepository;
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
        return shippingJobRepository.save(job);
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
        return shippingJobRepository.save(job);
    }

    @Transactional
    public ShippingJob removeOrder(String id, Long orderId) {
        ShippingJob job = editable(id);
        boolean removed = job.getOrders().removeIf(order -> orderId.equals(order.getOrderId()));
        if (!removed) throw new BizException("Outbound order is not bound to this shipping job");
        touchForUpdate(job);
        return shippingJobRepository.save(job);
    }

    @Transactional
    public ShippingJob schedule(String id) {
        ShippingJob job = editable(id);
        if (job.getOrders().isEmpty()) throw new BizException("Shipping job must contain at least one outbound order");
        if (!StringUtils.hasText(job.getTruckNo())) throw new BizException("Truck number is required before scheduling");
        job.setStatus(ShippingJobStatus.SCHEDULED);
        touchForUpdate(job);
        return shippingJobRepository.save(job);
    }

    @Transactional
    public ShippingJob ship(String id) {
        ShippingJob job = find(id);
        if (job.getStatus() != ShippingJobStatus.SCHEDULED) {
            throw new BizException("Only a scheduled shipping job can be shipped");
        }
        List<OutboundOrder> orders = outboundOrderRepository.findAllById(
                job.getOrders().stream().map(ShippingOrderRef::getOrderId).toList());
        if (orders.size() != job.getOrders().size()) throw new BizException("Some outbound orders no longer exist");
        List<String> unfinished = orders.stream()
                .filter(order -> order.getStatus() != OrderStatus.COMPLETED)
                .map(OutboundOrder::getOrderNo)
                .toList();
        if (!unfinished.isEmpty()) {
            throw new BizException("Outbound orders must be completed before shipping: " + String.join(", ", unfinished));
        }
        refreshOrderSnapshots(job, orders);
        job.setStatus(ShippingJobStatus.SHIPPED);
        job.setShippedAt(LocalDateTime.now());
        touchForUpdate(job);
        return shippingJobRepository.save(job);
    }

    @Transactional
    public ShippingJob cancel(String id) {
        ShippingJob job = find(id);
        if (job.getStatus() == ShippingJobStatus.SHIPPED) {
            throw new BizException("A shipped job cannot be cancelled");
        }
        job.setStatus(ShippingJobStatus.CANCELLED);
        touchForUpdate(job);
        return shippingJobRepository.save(job);
    }

    private ShippingJob editable(String id) {
        ShippingJob job = find(id);
        if (job.getStatus() != ShippingJobStatus.DRAFT) {
            throw new BizException("Only a draft shipping job can be edited");
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

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
