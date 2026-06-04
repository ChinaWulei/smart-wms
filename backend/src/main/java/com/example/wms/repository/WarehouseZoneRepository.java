package com.example.wms.repository;

import com.example.wms.domain.WarehouseZone;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseZoneRepository extends JpaRepository<WarehouseZone, Long> {
    Optional<WarehouseZone> findByWarehouseIdAndCode(Long warehouseId, String code);
}
