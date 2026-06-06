package com.example.wms.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "outbound_order_items")
public class OutboundOrderItem extends BaseEntity {
    @ManyToOne(optional = false)
    @JsonIgnore
    private OutboundOrder order;
    @ManyToOne(optional = false)
    private Product product;
    @ManyToOne(optional = false)
    private Warehouse warehouse;
    @ManyToOne(optional = false)
    private StorageLocation location;
    private Integer quantity;
    private Integer allocatedQuantity = 0;
    private Integer pickedQuantity = 0;

    public OutboundOrder getOrder() { return order; }
    public void setOrder(OutboundOrder order) { this.order = order; }
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
    public Integer getPickedQuantity() { return pickedQuantity; }
    public void setPickedQuantity(Integer pickedQuantity) { this.pickedQuantity = pickedQuantity; }
}
