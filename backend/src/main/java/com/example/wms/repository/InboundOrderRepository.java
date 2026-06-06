package com.example.wms.repository;

import com.example.wms.domain.InboundOrder;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InboundOrderRepository extends JpaRepository<InboundOrder, Long> {
    Optional<InboundOrder> findByOrderNo(String orderNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from InboundOrder o where o.orderNo = :orderNo")
    Optional<InboundOrder> findByOrderNoForUpdate(@Param("orderNo") String orderNo);
}
