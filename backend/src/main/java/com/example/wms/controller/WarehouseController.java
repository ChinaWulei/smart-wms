package com.example.wms.controller;

import com.example.wms.common.ApiResponse;
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
import com.example.wms.service.ShelfService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class WarehouseController {
    private final WarehouseRepository warehouseRepository;
    private final WarehouseZoneRepository zoneRepository;
    private final ShelfRepository shelfRepository;
    private final StorageLocationRepository locationRepository;
    private final ShelfService shelfService;

    public WarehouseController(WarehouseRepository warehouseRepository, WarehouseZoneRepository zoneRepository,
                               ShelfRepository shelfRepository, StorageLocationRepository locationRepository,
                               ShelfService shelfService) {
        this.warehouseRepository = warehouseRepository;
        this.zoneRepository = zoneRepository;
        this.shelfRepository = shelfRepository;
        this.locationRepository = locationRepository;
        this.shelfService = shelfService;
    }

    @GetMapping("/warehouses")
    public ApiResponse<List<Warehouse>> warehouses() { return ApiResponse.ok(warehouseRepository.findAll()); }

    @PostMapping("/warehouses")
    public ApiResponse<Warehouse> createWarehouse(@RequestBody Warehouse warehouse) { return ApiResponse.ok(warehouseRepository.save(warehouse)); }

    @PutMapping("/warehouses/{id}")
    public ApiResponse<Warehouse> updateWarehouse(@PathVariable Long id, @RequestBody Warehouse warehouse) {
        warehouse.setId(id);
        return ApiResponse.ok(warehouseRepository.save(warehouse));
    }

    @DeleteMapping("/warehouses/{id}")
    public ApiResponse<Void> deleteWarehouse(@PathVariable Long id) { warehouseRepository.deleteById(id); return ApiResponse.ok(); }

    @GetMapping("/zones")
    public ApiResponse<List<WarehouseZone>> zones() { return ApiResponse.ok(zoneRepository.findAll()); }

    @PostMapping("/zones")
    public ApiResponse<WarehouseZone> createZone(@RequestBody WarehouseZone zone) { return ApiResponse.ok(zoneRepository.save(zone)); }

    @GetMapping("/shelves")
    public ApiResponse<List<Shelf>> shelves() { return ApiResponse.ok(shelfRepository.findAll()); }

    @PostMapping("/shelves")
    public ApiResponse<Shelf> createShelf(@RequestBody Shelf shelf) { return ApiResponse.ok(shelfRepository.save(shelf)); }

    @PostMapping("/shelves/preview")
    public ApiResponse<ShelfPreviewResponse> previewShelf(@RequestBody ShelfGenerateRequest request) {
        return ApiResponse.ok(shelfService.preview(request));
    }

    @PostMapping("/shelves/generate")
    public ApiResponse<ShelfPreviewResponse> generateShelf(@RequestBody ShelfGenerateRequest request) {
        return ApiResponse.ok(shelfService.generate(request));
    }

    @GetMapping("/locations")
    public ApiResponse<List<LocationView>> locations(@RequestParam(required = false) Long warehouseId,
                                                     @RequestParam(required = false) Long shelfId,
                                                     @RequestParam(required = false) String code,
                                                     @RequestParam(required = false) String status) {
        return ApiResponse.ok(shelfService.queryLocations(warehouseId, shelfId, code, status));
    }

    @PostMapping("/locations")
    public ApiResponse<StorageLocation> createLocation(@RequestBody StorageLocation location) { return ApiResponse.ok(locationRepository.save(location)); }

    @PutMapping("/locations/{id}")
    public ApiResponse<StorageLocation> updateLocation(@PathVariable Long id, @RequestBody StorageLocation location) {
        location.setId(id);
        return ApiResponse.ok(locationRepository.save(location));
    }
}
