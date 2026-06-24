package com.example.wms.shipping;

import java.util.List;
import java.util.Optional;
import java.util.Collection;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ShippingJobRepository extends MongoRepository<ShippingJob, String> {
    List<ShippingJob> findAllByOrderByCreatedAtDesc();
    List<ShippingJob> findByWarehouseIdOrderByCreatedAtDesc(Long warehouseId);
    List<ShippingJob> findByStatusInOrderByCreatedAtDesc(Collection<ShippingJobStatus> statuses);
    List<ShippingJob> findByWarehouseIdAndStatusInOrderByCreatedAtDesc(Long warehouseId, Collection<ShippingJobStatus> statuses);
    Optional<ShippingJob> findByJobNo(String jobNo);
    Optional<ShippingJob> findTopByWarehouseCode3OrderBySequenceDesc(String warehouseCode3);

    Optional<ShippingJob> findFirstByOrdersOrderIdAndStatusNot(Long orderId, ShippingJobStatus cancelledStatus);
}
