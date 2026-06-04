package com.example.wms.service;

import com.example.wms.common.BizException;
import com.example.wms.domain.Shelf;
import com.example.wms.domain.StorageLocation;
import com.example.wms.domain.Warehouse;
import com.example.wms.domain.WarehouseZone;
import com.example.wms.dto.WmsDtos.LocationView;
import com.example.wms.dto.WmsDtos.ShelfGenerateRequest;
import com.example.wms.dto.WmsDtos.ShelfPreviewResponse;
import com.example.wms.repository.ShelfRepository;
import com.example.wms.repository.StorageLocationRepository;
import com.example.wms.repository.WarehouseRepository;
import com.example.wms.repository.WarehouseZoneRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ShelfService {
    private final WarehouseRepository warehouseRepository;
    private final WarehouseZoneRepository zoneRepository;
    private final ShelfRepository shelfRepository;
    private final StorageLocationRepository locationRepository;

    public ShelfService(WarehouseRepository warehouseRepository, WarehouseZoneRepository zoneRepository,
                        ShelfRepository shelfRepository, StorageLocationRepository locationRepository) {
        this.warehouseRepository = warehouseRepository;
        this.zoneRepository = zoneRepository;
        this.shelfRepository = shelfRepository;
        this.locationRepository = locationRepository;
    }

    public ShelfPreviewResponse preview(ShelfGenerateRequest request) {
        validateRequest(request);
        List<String> codes = buildCodes(request.shelfCode(), request.xCount(), request.yCount(), request.zCount());
        return new ShelfPreviewResponse(codes, codes.size());
    }

    @Transactional
    public ShelfPreviewResponse generate(ShelfGenerateRequest request) {
        validateRequest(request);
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new BizException("Warehouse not found"));
        shelfRepository.findByZoneWarehouseIdAndCode(warehouse.getId(), request.shelfCode()).ifPresent(s -> {
            throw new BizException("Shelf code already exists in this warehouse");
        });
        WarehouseZone zone = zoneRepository.findByWarehouseIdAndCode(warehouse.getId(), "DEFAULT")
                .orElseGet(() -> {
                    WarehouseZone created = new WarehouseZone();
                    created.setWarehouse(warehouse);
                    created.setCode("DEFAULT");
                    created.setName(warehouse.getName() + " DEFAULT");
                    return zoneRepository.save(created);
                });
        Shelf shelf = new Shelf();
        shelf.setCode(request.shelfCode());
        shelf.setName(StringUtils.hasText(request.shelfName()) ? request.shelfName() : request.shelfCode() + " shelf");
        shelf.setZone(zone);
        shelf = shelfRepository.save(shelf);

        List<String> codes = buildCodes(request.shelfCode(), request.xCount(), request.yCount(), request.zCount());
        List<StorageLocation> locations = new ArrayList<>();
        for (String code : codes) {
            StorageLocation location = new StorageLocation();
            location.setCode(code);
            location.setWarehouse(warehouse);
            location.setShelf(shelf);
            location.setCapacity(request.capacity() == null ? 0 : request.capacity());
            location.setOccupied(0);
            locations.add(location);
        }
        locationRepository.saveAll(locations);
        return new ShelfPreviewResponse(codes, codes.size());
    }

    public List<LocationView> queryLocations(Long warehouseId, Long shelfId, String code, String status) {
        return locationRepository.findAll().stream()
                .filter(l -> warehouseId == null || l.getWarehouse().getId().equals(warehouseId))
                .filter(l -> shelfId == null || (l.getShelf() != null && l.getShelf().getId().equals(shelfId)))
                .filter(l -> !StringUtils.hasText(code) || l.getCode().contains(code.trim()))
                .filter(l -> !StringUtils.hasText(status) || l.getStatus().name().equals(status))
                .map(l -> new LocationView(l.getId(), l.getCode(), l.getWarehouse().getId(), l.getWarehouse().getName(),
                        l.getShelf() == null ? null : l.getShelf().getId(),
                        l.getShelf() == null ? "" : l.getShelf().getCode(),
                        l.getCapacity(), l.getOccupied(), l.getStatus()))
                .toList();
    }

    private void validateRequest(ShelfGenerateRequest request) {
        if (request.warehouseId() == null) throw new BizException("Warehouse is required");
        if (!StringUtils.hasText(request.shelfCode())) throw new BizException("Shelf code is required");
        if (!positive(request.xCount()) || !positive(request.yCount()) || !positive(request.zCount())) {
            throw new BizException("X, Y and Z must be positive integers");
        }
        if (request.xCount() * request.yCount() * request.zCount() > 500) {
            throw new BizException("Cannot generate more than 500 locations at once");
        }
    }

    private boolean positive(Integer value) {
        return value != null && value > 0;
    }

    private List<String> buildCodes(String shelfCode, int xCount, int yCount, int zCount) {
        List<String> codes = new ArrayList<>();
        for (int x = 1; x <= xCount; x++) {
            for (int y = 1; y <= yCount; y++) {
                for (int z = 1; z <= zCount; z++) {
                    codes.add("LT-" + shelfCode.trim() + "-" + x + "-" + y + "-" + z);
                }
            }
        }
        return codes;
    }
}
