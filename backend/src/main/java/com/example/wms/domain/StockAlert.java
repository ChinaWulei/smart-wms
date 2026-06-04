package com.example.wms.domain;

import com.example.wms.domain.enums.AlertStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock_alerts")
public class StockAlert extends BaseEntity {
    @ManyToOne(optional = false)
    private Product product;
    @Enumerated(EnumType.STRING)
    private AlertStatus status = AlertStatus.OPEN;
    private String message;

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public AlertStatus getStatus() { return status; }
    public void setStatus(AlertStatus status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
