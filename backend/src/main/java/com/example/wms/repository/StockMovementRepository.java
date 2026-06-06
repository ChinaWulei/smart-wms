package com.example.wms.repository;

import com.example.wms.domain.StockMovement;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    boolean existsByProductId(Long productId);
    List<StockMovement> findTop200ByOrderByMovementTimeDesc();
    List<StockMovement> findBySourceNoOrderByMovementTimeDesc(String sourceNo);

    @Query("select coalesce(sum(m.quantity), 0) from StockMovement m where m.type = com.example.wms.domain.enums.MovementType.INBOUND and m.movementTime >= :start")
    long todayInbound(LocalDateTime start);

    @Query("select coalesce(sum(m.quantity), 0) from StockMovement m where m.type = com.example.wms.domain.enums.MovementType.OUTBOUND and m.movementTime >= :start")
    long todayOutbound(LocalDateTime start);

    @Query("select m.product.name, coalesce(sum(m.quantity), 0) from StockMovement m where m.type = com.example.wms.domain.enums.MovementType.OUTBOUND and m.movementTime >= :start group by m.product.id, m.product.name order by sum(m.quantity) desc")
    List<Object[]> topOutbound(LocalDateTime start);

    @Query("select function('date', m.movementTime), sum(case when m.type = com.example.wms.domain.enums.MovementType.INBOUND then m.quantity else 0 end), sum(case when m.type = com.example.wms.domain.enums.MovementType.OUTBOUND then m.quantity else 0 end) from StockMovement m where m.movementTime >= :start group by function('date', m.movementTime) order by function('date', m.movementTime)")
    List<Object[]> trend(LocalDateTime start);
}
