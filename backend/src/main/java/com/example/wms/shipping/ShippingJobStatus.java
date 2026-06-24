package com.example.wms.shipping;

public enum ShippingJobStatus {
    DRAFT,
    SCHEDULED,
    IN_QUEUE,
    READY_TO_SORT,
    SORTING,
    SHIPPED,
    COMPLETED,
    CANCELLED
}
