package com.example.wms.shipping;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public final class PdaDtos {
    private PdaDtos() {}

    public record PdaShippingJobView(
            ShippingJob shippingJob,
            List<ShippingOrder> orders
    ) {}

    public record ScanRequest(
            @NotBlank String code,
            Integer quantity,
            String operatorName
    ) {}

    public record ScanResponse(
            String message,
            ShippingOrder order,
            PdaShippingJobView job
    ) {}
}
