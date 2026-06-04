package com.example.wms.repository;

import com.example.wms.domain.Shelf;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShelfRepository extends JpaRepository<Shelf, Long> {
    Optional<Shelf> findByZoneWarehouseIdAndCode(Long warehouseId, String code);
}
