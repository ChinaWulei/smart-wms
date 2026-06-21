package com.example.wms.shipping;

import com.example.wms.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "shipping_jobs")
public class ShippingJob extends BaseEntity {
    @Version
    private Long version;
    @Column(nullable = false, unique = true)
    private String jobNo;
    @Column(nullable = false)
    private Long warehouseId;
    private String warehouseCode;
    @Column(nullable = false)
    private LocalDate plannedShipDate;
    private String truckNo;
    private String driverName;
    private String driverPhone;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShippingJobStatus status = ShippingJobStatus.DRAFT;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<ShippingOrderRef> orders = new ArrayList<>();
    private String remark;
    private String createdBy;
    private LocalDateTime shippedAt;

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public String getJobNo() { return jobNo; }
    public void setJobNo(String jobNo) { this.jobNo = jobNo; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
    public LocalDate getPlannedShipDate() { return plannedShipDate; }
    public void setPlannedShipDate(LocalDate plannedShipDate) { this.plannedShipDate = plannedShipDate; }
    public String getTruckNo() { return truckNo; }
    public void setTruckNo(String truckNo) { this.truckNo = truckNo; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
    public String getDriverPhone() { return driverPhone; }
    public void setDriverPhone(String driverPhone) { this.driverPhone = driverPhone; }
    public ShippingJobStatus getStatus() { return status; }
    public void setStatus(ShippingJobStatus status) { this.status = status; }
    public List<ShippingOrderRef> getOrders() { return orders; }
    public void setOrders(List<ShippingOrderRef> orders) { this.orders = orders; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getShippedAt() { return shippedAt; }
    public void setShippedAt(LocalDateTime shippedAt) { this.shippedAt = shippedAt; }

    public static class ShippingOrderRef {
        private Long orderId;
        private String orderNo;
        private String receiverName;
        private String address;
        private String orderStatus;
        private Integer itemCount;

        public ShippingOrderRef() {}

        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
        public String getOrderNo() { return orderNo; }
        public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
        public String getReceiverName() { return receiverName; }
        public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getOrderStatus() { return orderStatus; }
        public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
        public Integer getItemCount() { return itemCount; }
        public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
    }
}
