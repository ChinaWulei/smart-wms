package com.example.wms.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "shelves")
public class Shelf extends BaseEntity {
    private String code;
    private String name;
    @ManyToOne(optional = false)
    private WarehouseZone zone;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public WarehouseZone getZone() { return zone; }
    public void setZone(WarehouseZone zone) { this.zone = zone; }
}
