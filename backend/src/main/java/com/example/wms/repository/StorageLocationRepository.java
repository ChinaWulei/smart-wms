package com.example.wms.repository;

import com.example.wms.domain.StorageLocation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorageLocationRepository extends JpaRepository<StorageLocation, Long> {
    List<StorageLocation> findByWarehouseId(Long warehouseId);
    Optional<StorageLocation> findByCode(String code);
    List<StorageLocation> findByShelfId(Long shelfId);
}
