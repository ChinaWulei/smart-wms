package com.example.wms.repository;

import com.example.wms.domain.InboundOrder;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboundOrderRepository extends JpaRepository<InboundOrder, Long> {
    Optional<InboundOrder> findByOrderNo(String orderNo);
}
