package com.example.wms.domain.enums;

public enum OrderStatus {
    CREATED,
    IN_QUEUE,
    RECEIVING,
    RECEIVED,
    ALLOCATED,
    NOT_ENOUGH_INV,
    READY_TO_PICK,
    PICKING,
    PICKED,
    COMPLETED,
    CANCELLED
}
