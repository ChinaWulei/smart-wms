package com.example.wms.repository;

import com.example.wms.domain.Stock;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByProductIdAndWarehouseIdAndLocationId(Long productId, Long warehouseId, Long locationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.product.id = :productId and s.warehouse.id = :warehouseId and s.location.id = :locationId")
    Optional<Stock> findForUpdate(@Param("productId") Long productId, @Param("warehouseId") Long warehouseId,
                                  @Param("locationId") Long locationId);
    List<Stock> findByProductId(Long productId);
    List<Stock> findByWarehouseId(Long warehouseId);
    List<Stock> findByLocationId(Long locationId);

    @Query("select coalesce(sum(s.quantity), 0) from Stock s where s.product.id = :productId")
    int sumQuantityByProductId(Long productId);

    @Query("select coalesce(sum(s.quantity), 0) from Stock s")
    long totalQuantity();
}
