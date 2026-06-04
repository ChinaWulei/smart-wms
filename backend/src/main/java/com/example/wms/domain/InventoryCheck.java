package com.example.wms.domain;

import com.example.wms.domain.enums.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inventory_checks")
public class InventoryCheck extends BaseEntity {
    private String checkNo;
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.CREATED;
    private String operatorName;
    private LocalDateTime confirmedAt;
    private String remark;
    @OneToMany(mappedBy = "checkTask", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InventoryCheckItem> items = new ArrayList<>();

    public String getCheckNo() { return checkNo; }
    public void setCheckNo(String checkNo) { this.checkNo = checkNo; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public List<InventoryCheckItem> getItems() { return items; }
    public void setItems(List<InventoryCheckItem> items) { this.items = items; }
}
