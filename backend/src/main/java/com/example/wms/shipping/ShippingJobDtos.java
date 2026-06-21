package com.example.wms.shipping;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public final class ShippingJobDtos {
    private ShippingJobDtos() {}

    public record CreateShippingJobRequest(
            @NotNull Long warehouseId,
            @NotNull LocalDate plannedShipDate,
            String truckNo,
            String driverName,
            String driverPhone,
            String remark,
            String createdBy,
            List<Long> outboundOrderIds
    ) {}

    public record AddOrdersRequest(@NotEmpty List<Long> outboundOrderIds) {}

    public record UpdateShippingJobRequest(
            @NotNull LocalDate plannedShipDate,
            String truckNo,
            String driverName,
            String driverPhone,
            String remark
    ) {}
}
