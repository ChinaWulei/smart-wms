package com.example.wms.repository;

import com.example.wms.domain.OutboundOrder;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboundOrderRepository extends JpaRepository<OutboundOrder, Long> {
    Optional<OutboundOrder> findByOrderNo(String orderNo);
}
