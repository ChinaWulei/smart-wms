package com.example.wms.shipping;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShippingJobRepository extends JpaRepository<ShippingJob, Long> {
    List<ShippingJob> findAllByOrderByPlannedShipDateDescCreatedAtDesc();
    List<ShippingJob> findByWarehouseIdOrderByPlannedShipDateDescCreatedAtDesc(Long warehouseId);

    @Query(value = """
            select *
            from shipping_jobs job
            where job.status <> :cancelledStatus
              and exists (
                select 1
                from jsonb_array_elements(job.orders) item
                where cast(item ->> 'orderId' as bigint) = :orderId
              )
            limit 1
            """, nativeQuery = true)
    Optional<ShippingJob> findActiveByOutboundOrderId(
            @Param("orderId") Long orderId,
            @Param("cancelledStatus") String cancelledStatus);
}
