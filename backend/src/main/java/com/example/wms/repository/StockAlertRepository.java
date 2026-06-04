package com.example.wms.repository;

import com.example.wms.domain.StockAlert;
import com.example.wms.domain.enums.AlertStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockAlertRepository extends JpaRepository<StockAlert, Long> {
    long countByStatusNot(AlertStatus status);
    List<StockAlert> findByStatus(AlertStatus status);
    Optional<StockAlert> findFirstByProductIdAndStatusNot(Long productId, AlertStatus status);
}
