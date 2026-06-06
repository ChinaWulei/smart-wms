package com.example.wms.repository;

import com.example.wms.domain.OutboundOrder;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboundOrderRepository extends JpaRepository<OutboundOrder, Long> {
    Optional<OutboundOrder> findByOrderNo(String orderNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OutboundOrder o where o.id = :id")
    Optional<OutboundOrder> findByIdForUpdate(@Param("id") Long id);
}
