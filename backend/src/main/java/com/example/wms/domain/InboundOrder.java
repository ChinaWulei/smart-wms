package com.example.wms.domain;

import com.example.wms.domain.enums.InboundType;
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
@Table(name = "inbound_orders")
public class InboundOrder extends BaseEntity {
    private String orderNo;
    @Enumerated(EnumType.STRING)
    private InboundType type;
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.IN_QUEUE;
    private String operatorName;
    private LocalDateTime receivingStartedAt;
    private LocalDateTime receivedAt;
    private LocalDateTime completedAt;
    private String completedBy;
    private LocalDateTime cancelledAt;
    private String cancelledBy;
    private String remark;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InboundOrderItem> items = new ArrayList<>();

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public InboundType getType() { return type; }
    public void setType(InboundType type) { this.type = type; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public LocalDateTime getReceivingStartedAt() { return receivingStartedAt; }
    public void setReceivingStartedAt(LocalDateTime receivingStartedAt) { this.receivingStartedAt = receivingStartedAt; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public String getCompletedBy() { return completedBy; }
    public void setCompletedBy(String completedBy) { this.completedBy = completedBy; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public List<InboundOrderItem> getItems() { return items; }
    public void setItems(List<InboundOrderItem> items) { this.items = items; }
}
