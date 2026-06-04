package com.example.wms.repository;

import com.example.wms.domain.Stock;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByProductIdAndWarehouseIdAndLocationId(Long productId, Long warehouseId, Long locationId);
    List<Stock> findByProductId(Long productId);
    List<Stock> findByWarehouseId(Long warehouseId);
    List<Stock> findByLocationId(Long locationId);

    @Query("select coalesce(sum(s.quantity), 0) from Stock s where s.product.id = :productId")
    int sumQuantityByProductId(Long productId);

    @Query("select coalesce(sum(s.quantity), 0) from Stock s")
    long totalQuantity();
}
