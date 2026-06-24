package com.example.wms.shipping;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "shipping_order")
@CompoundIndexes({
        @CompoundIndex(name = "idx_shipping_order_job_order", def = "{'shippingJobId': 1, 'orderId': 1}", unique = true),
        @CompoundIndex(name = "idx_shipping_order_job_status", def = "{'shippingJobId': 1, 'status': 1}"),
        @CompoundIndex(name = "idx_shipping_order_items_sku", def = "{'items.sku': 1}"),
        @CompoundIndex(name = "idx_shipping_order_items_barcode", def = "{'items.barcode': 1}")
})
public class ShippingOrder {
    @Id
    private String id;
    @Version
    private Long version;
    @Indexed
    private String shippingJobId;
    @Indexed
    private String jobNo;
    private Long orderId;
    private String orderNo;
    private Long warehouseId;
    private String receiverName;
    private String receiverPhone;
    private String address;
    private ShippingOrderStatus status = ShippingOrderStatus.PENDING;
    private List<Item> items = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public String getShippingJobId() { return shippingJobId; }
    public void setShippingJobId(String shippingJobId) { this.shippingJobId = shippingJobId; }
    public String getJobNo() { return jobNo; }
    public void setJobNo(String jobNo) { this.jobNo = jobNo; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public ShippingOrderStatus getStatus() { return status; }
    public void setStatus(ShippingOrderStatus status) { this.status = status; }
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public static class Item {
        private Long productId;
        private String sku;
        private String barcode;
        private String productName;
        private String locationCode;
        private Integer requiredQuantity = 0;
        private Integer scannedQuantity = 0;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        public String getBarcode() { return barcode; }
        public void setBarcode(String barcode) { this.barcode = barcode; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getLocationCode() { return locationCode; }
        public void setLocationCode(String locationCode) { this.locationCode = locationCode; }
        public Integer getRequiredQuantity() { return requiredQuantity; }
        public void setRequiredQuantity(Integer requiredQuantity) { this.requiredQuantity = requiredQuantity; }
        public Integer getScannedQuantity() { return scannedQuantity; }
        public void setScannedQuantity(Integer scannedQuantity) { this.scannedQuantity = scannedQuantity; }
    }
}
