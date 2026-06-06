package com.example.wms.domain;

import com.example.wms.domain.enums.OrderStatus;
import com.example.wms.domain.enums.OutboundType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "outbound_orders")
public class OutboundOrder extends BaseEntity {
    private String orderNo;
    @Enumerated(EnumType.STRING)
    private OutboundType type;
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.CREATED;
    private String operatorName;
    private String receiverName;
    private String receiverPhone;
    @Column(length = 500)
    private String address;
    private String reason;
    private String trackingNo;
    private LocalDateTime completedAt;
    private String completedBy;
    private LocalDateTime cancelledAt;
    private String cancelledBy;
    private String remark;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OutboundOrderItem> items = new ArrayList<>();

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public OutboundType getType() { return type; }
    public void setType(OutboundType type) { this.type = type; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getTrackingNo() { return trackingNo; }
    public void setTrackingNo(String trackingNo) { this.trackingNo = trackingNo; }
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
    public List<OutboundOrderItem> getItems() { return items; }
    public void setItems(List<OutboundOrderItem> items) { this.items = items; }
}
