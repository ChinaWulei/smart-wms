package com.example.wms.shipping;

import com.example.wms.common.BizException;
import com.example.wms.domain.OutboundOrder;
import com.example.wms.domain.enums.OrderStatus;
import com.example.wms.repository.OutboundOrderRepository;
import com.example.wms.shipping.PdaDtos.PdaShippingJobView;
import com.example.wms.shipping.PdaDtos.ScanResponse;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PdaShippingService {
    private final ShippingJobRepository shippingJobRepository;
    private final ShippingOrderRepository shippingOrderRepository;
    private final OutboundOrderRepository outboundOrderRepository;

    public PdaShippingService(ShippingJobRepository shippingJobRepository,
                              ShippingOrderRepository shippingOrderRepository,
                              OutboundOrderRepository outboundOrderRepository) {
        this.shippingJobRepository = shippingJobRepository;
        this.shippingOrderRepository = shippingOrderRepository;
        this.outboundOrderRepository = outboundOrderRepository;
    }

    @Transactional(readOnly = true)
    public List<ShippingJob> list(Long warehouseId) {
        var statuses = EnumSet.of(
                ShippingJobStatus.READY_TO_SORT,
                ShippingJobStatus.SORTING,
                ShippingJobStatus.SHIPPED,
                ShippingJobStatus.SCHEDULED
        );
        return warehouseId == null
                ? shippingJobRepository.findByStatusInOrderByCreatedAtDesc(statuses)
                : shippingJobRepository.findByWarehouseIdAndStatusInOrderByCreatedAtDesc(warehouseId, statuses);
    }

    @Transactional
    public PdaShippingJobView load(String jobNo) {
        ShippingJob job = findJob(jobNo);
        if (job.getStatus() == ShippingJobStatus.IN_QUEUE || job.getStatus() == ShippingJobStatus.DRAFT) {
            throw new BizException("Shipping Job 还未 Start to Ship，不能在 App 端操作");
        }
        if (job.getStatus() == ShippingJobStatus.CANCELLED) {
            throw new BizException("Shipping Job 已取消，不能在 App 端操作");
        }
        if (job.getStatus() == ShippingJobStatus.SHIPPED) {
            throw new BizException("Shipping Job 已发运完成");
        }
        if (job.getStatus() == ShippingJobStatus.READY_TO_SORT || job.getStatus() == ShippingJobStatus.SCHEDULED) {
            job.setStatus(ShippingJobStatus.SORTING);
            job.setUpdatedAt(LocalDateTime.now());
            shippingJobRepository.save(job);
        }
        return view(job);
    }

    @Transactional
    public ScanResponse scan(String jobNo, String code, Integer quantity) {
        ShippingJob job = findJob(jobNo);
        if (job.getStatus() != ShippingJobStatus.SORTING) {
            throw new BizException("Shipping Job 当前状态不是 Sorting，不能扫码");
        }
        String normalizedCode = normalize(code);
        int scanQuantity = Math.max(1, quantity == null ? 1 : quantity);
        List<ShippingOrder> orders = shippingOrderRepository.findByShippingJobIdOrderByOrderNoAsc(job.getId());
        ShippingOrder matchedOrder = null;
        ShippingOrder.Item matchedItem = null;
        for (ShippingOrder order : orders) {
            for (ShippingOrder.Item item : order.getItems()) {
                if (matches(item, normalizedCode)) {
                    matchedOrder = order;
                    matchedItem = item;
                    break;
                }
            }
            if (matchedItem != null) break;
        }
        if (matchedOrder == null || matchedItem == null) {
            throw new BizException("该商品不属于当前 Shipping Job：" + code);
        }
        int required = ShippingJobService.safe(matchedItem.getRequiredQuantity());
        int scanned = ShippingJobService.safe(matchedItem.getScannedQuantity());
        if (scanned >= required) {
            throw new BizException("该商品已扫够数量：" + matchedItem.getSku());
        }
        if (scanned + scanQuantity > required) {
            throw new BizException("扫码数量超出应发数量，剩余：" + (required - scanned));
        }
        matchedItem.setScannedQuantity(scanned + scanQuantity);
        matchedOrder.setUpdatedAt(LocalDateTime.now());
        ShippingJobService.updateShippingOrderStatus(matchedOrder);
        shippingOrderRepository.save(matchedOrder);
        return new ScanResponse("扫码成功：" + matchedItem.getSku(), matchedOrder, view(job));
    }

    @Transactional
    public PdaShippingJobView complete(String jobNo) {
        ShippingJob job = findJob(jobNo);
        List<ShippingOrder> shippingOrders = shippingOrderRepository.findByShippingJobIdOrderByOrderNoAsc(job.getId());
        if (shippingOrders.isEmpty()) throw new BizException("当前 Shipping Job 没有发运订单");
        boolean allScanned = shippingOrders.stream().allMatch(order -> order.getStatus() == ShippingOrderStatus.DONE);
        if (!allScanned) throw new BizException("还有订单未完成扫码，不能确认发运");
        List<OutboundOrder> outboundOrders = outboundOrderRepository.findAllById(
                shippingOrders.stream().map(ShippingOrder::getOrderId).toList());
        List<String> unfinished = outboundOrders.stream()
                .filter(order -> !ShippingJobService.isPacked(order.getStatus()))
                .map(OutboundOrder::getOrderNo)
                .toList();
        if (!unfinished.isEmpty()) {
            throw new BizException("出库单未 Packed，不能发运：" + String.join(", ", unfinished));
        }
        job.setStatus(ShippingJobStatus.SHIPPED);
        job.setShippedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        shippingJobRepository.save(job);
        return view(job);
    }

    private PdaShippingJobView view(ShippingJob job) {
        return new PdaShippingJobView(job, shippingOrderRepository.findByShippingJobIdOrderByOrderNoAsc(job.getId()));
    }

    private ShippingJob findJob(String jobNo) {
        return shippingJobRepository.findByJobNo(normalize(jobNo))
                .orElseThrow(() -> new BizException("Shipping Job 不存在：" + jobNo));
    }

    private boolean matches(ShippingOrder.Item item, String code) {
        return normalize(item.getSku()).equals(code) || normalize(item.getBarcode()).equals(code);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : "";
    }
}
