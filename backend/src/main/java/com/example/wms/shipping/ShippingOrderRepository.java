package com.example.wms.shipping;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ShippingOrderRepository extends MongoRepository<ShippingOrder, String> {
    List<ShippingOrder> findByShippingJobIdOrderByOrderNoAsc(String shippingJobId);
    List<ShippingOrder> findByJobNoOrderByOrderNoAsc(String jobNo);
    Optional<ShippingOrder> findByShippingJobIdAndOrderId(String shippingJobId, Long orderId);
    void deleteByShippingJobIdAndOrderId(String shippingJobId, Long orderId);
    void deleteByShippingJobIdAndOrderIdIn(String shippingJobId, Collection<Long> orderIds);
    boolean existsByShippingJobIdAndStatusNot(String shippingJobId, ShippingOrderStatus status);
}
