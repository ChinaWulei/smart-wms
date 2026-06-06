package com.example.wms.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "stocks", uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "warehouse_id", "location_id"}))
public class Stock extends BaseEntity {
    @ManyToOne(optional = false)
    private Product product;
    @ManyToOne(optional = false)
    private Warehouse warehouse;
    @ManyToOne(optional = false)
    private StorageLocation location;
    private Integer quantity = 0;
    private Integer allocatedQuantity = 0;

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }
    public StorageLocation getLocation() { return location; }
    public void setLocation(StorageLocation location) { this.location = location; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getAllocatedQuantity() { return allocatedQuantity; }
    public void setAllocatedQuantity(Integer allocatedQuantity) { this.allocatedQuantity = allocatedQuantity; }
}
