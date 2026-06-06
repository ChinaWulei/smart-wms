package com.example.wms.domain.enums;

public enum OrderStatus {
    CREATED,
    IN_QUEUE,
    RECEIVING,
    RECEIVED,
    ALLOCATED,
    READY_TO_PICK,
    PICKING,
    PICKED,
    COMPLETED,
    CANCELLED
}
