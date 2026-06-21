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
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
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
        job.setJobNo(nextJobNo(now));
        job.setWarehouseId(warehouse.getId());
        job.setWarehouseCode(warehouse.getCode());
        job.setPlannedShipDate(request.plannedShipDate());
        job.setTruckNo(trim(request.truckNo()));
        job.setDriverName(trim(request.driverName()));
        job.setDriverPhone(trim(request.driverPhone()));
        job.setRemark(trim(request.remark()));
        job.setCreatedBy(trim(request.createdBy()));
        attachOrders(job, request.outboundOrderIds());
        return shippingJobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public List<ShippingJob> list(Long warehouseId) {
        return warehouseId == null
                ? shippingJobRepository.findAllByOrderByPlannedShipDateDescCreatedAtDesc()
                : shippingJobRepository.findByWarehouseIdOrderByPlannedShipDateDescCreatedAtDesc(warehouseId);
    }

    @Transactional(readOnly = true)
    public ShippingJob get(Long id) {
        return find(id);
    }

    @Transactional
    public ShippingJob update(Long id, UpdateShippingJobRequest request) {
        ShippingJob job = editable(id);
        job.setPlannedShipDate(request.plannedShipDate());
        job.setTruckNo(trim(request.truckNo()));
        job.setDriverName(trim(request.driverName()));
        job.setDriverPhone(trim(request.driverPhone()));
        job.setRemark(trim(request.remark()));
        return shippingJobRepository.save(job);
    }

    @Transactional
    public ShippingJob addOrders(Long id, Collection<Long> orderIds) {
        ShippingJob job = editable(id);
        attachOrders(job, orderIds);
        return shippingJobRepository.save(job);
    }

    @Transactional
    public ShippingJob removeOrder(Long id, Long orderId) {
        ShippingJob job = editable(id);
        boolean removed = job.getOrders().removeIf(order -> orderId.equals(order.getOrderId()));
        if (!removed) throw new BizException("Outbound order is not bound to this shipping job");
        return shippingJobRepository.save(job);
    }

    @Transactional
    public ShippingJob schedule(Long id) {
        ShippingJob job = editable(id);
        if (job.getOrders().isEmpty()) throw new BizException("Shipping job must contain at least one outbound order");
        if (!StringUtils.hasText(job.getTruckNo())) throw new BizException("Truck number is required before scheduling");
        job.setStatus(ShippingJobStatus.SCHEDULED);
        return shippingJobRepository.save(job);
    }

    @Transactional
    public ShippingJob ship(Long id) {
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
        return shippingJobRepository.save(job);
    }

    @Transactional
    public ShippingJob cancel(Long id) {
        ShippingJob job = find(id);
        if (job.getStatus() == ShippingJobStatus.SHIPPED) {
            throw new BizException("A shipped job cannot be cancelled");
        }
        job.setStatus(ShippingJobStatus.CANCELLED);
        return shippingJobRepository.save(job);
    }

    private ShippingJob editable(Long id) {
        ShippingJob job = find(id);
        if (job.getStatus() != ShippingJobStatus.DRAFT) {
            throw new BizException("Only a draft shipping job can be edited");
        }
        return job;
    }

    private ShippingJob find(Long id) {
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
        shippingJobRepository.findActiveByOutboundOrderId(order.getId(), ShippingJobStatus.CANCELLED.name())
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

    private String nextJobNo(LocalDateTime now) {
        return "SJ" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
