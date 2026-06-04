package com.example.wms.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "inventory_check_items")
public class InventoryCheckItem extends BaseEntity {
    @ManyToOne(optional = false)
    @JsonIgnore
    private InventoryCheck checkTask;
    @ManyToOne(optional = false)
    private Product product;
    @ManyToOne(optional = false)
    private Warehouse warehouse;
    @ManyToOne(optional = false)
    private StorageLocation location;
    private Integer bookQuantity;
    private Integer actualQuantity;
    private Integer diffQuantity;

    public InventoryCheck getCheckTask() { return checkTask; }
    public void setCheckTask(InventoryCheck checkTask) { this.checkTask = checkTask; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }
    public StorageLocation getLocation() { return location; }
    public void setLocation(StorageLocation location) { this.location = location; }
    public Integer getBookQuantity() { return bookQuantity; }
    public void setBookQuantity(Integer bookQuantity) { this.bookQuantity = bookQuantity; }
    public Integer getActualQuantity() { return actualQuantity; }
    public void setActualQuantity(Integer actualQuantity) { this.actualQuantity = actualQuantity; }
    public Integer getDiffQuantity() { return diffQuantity; }
    public void setDiffQuantity(Integer diffQuantity) { this.diffQuantity = diffQuantity; }
}
