package com.example.wms.shipping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "shipping_job")
@CompoundIndexes({
        @CompoundIndex(name = "idx_shipping_job_warehouse_status_created",
                def = "{'warehouseId': 1, 'status': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "idx_shipping_job_warehouse_planned_created",
                def = "{'warehouseId': 1, 'plannedShipDate': -1, 'createdAt': -1}"),
        @CompoundIndex(name = "idx_shipping_job_orders_order_id",
                def = "{'orders.orderId': 1}")
})
public class ShippingJob {
    @Id
    private String id;
    @Version
    private Long version;
    @Indexed(unique = true)
    private String jobNo;
    private Long warehouseId;
    private String warehouseCode;
    @Indexed
    private String warehouseCode3;
    private Long sequence;
    private LocalDate plannedShipDate;
    private String truckNo;
    private String driverName;
    private String driverPhone;
    private ShippingJobStatus status = ShippingJobStatus.DRAFT;
    private List<ShippingOrderRef> orders = new ArrayList<>();
    private String remark;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime shippedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public String getJobNo() { return jobNo; }
    public void setJobNo(String jobNo) { this.jobNo = jobNo; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
    public String getWarehouseCode3() { return warehouseCode3; }
    public void setWarehouseCode3(String warehouseCode3) { this.warehouseCode3 = warehouseCode3; }
    public Long getSequence() { return sequence; }
    public void setSequence(Long sequence) { this.sequence = sequence; }
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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
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
