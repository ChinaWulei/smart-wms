package com.example.wms.domain;

import com.example.wms.domain.enums.LocationStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "storage_locations", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
public class StorageLocation extends BaseEntity {
    private String code;
    @ManyToOne(optional = false)
    private Warehouse warehouse;
    @ManyToOne
    private Shelf shelf;
    private Integer capacity = 0;
    private Integer occupied = 0;
    @Enumerated(EnumType.STRING)
    private LocationStatus status = LocationStatus.ENABLED;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }
    public Shelf getShelf() { return shelf; }
    public void setShelf(Shelf shelf) { this.shelf = shelf; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public Integer getOccupied() { return occupied; }
    public void setOccupied(Integer occupied) { this.occupied = occupied; }
    public LocationStatus getStatus() { return status; }
    public void setStatus(LocationStatus status) { this.status = status; }
}
