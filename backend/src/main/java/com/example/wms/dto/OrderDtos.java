package com.example.wms.dto;

import com.example.wms.domain.enums.InboundType;
import com.example.wms.domain.enums.OutboundType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class OrderDtos {
    public record LineItem(
            @NotNull Long productId,
            @NotNull Long warehouseId,
            @NotNull Long locationId,
            @NotNull @Min(1) Integer quantity
    ) {}

    public record InboundRequest(
            @NotNull InboundType type,
            String operatorName,
            String remark,
            @NotEmpty @Valid List<LineItem> items
    ) {}

    public record OutboundRequest(
            @NotNull OutboundType type,
            String operatorName,
            String remark,
            @NotEmpty @Valid List<LineItem> items
    ) {}

    public record CheckRequest(
            String operatorName,
            String remark,
            @NotEmpty @Valid List<CheckLineItem> items
    ) {}

    public record CheckLineItem(
            @NotNull Long productId,
            @NotNull Long warehouseId,
            @NotNull Long locationId,
            @NotNull @Min(0) Integer actualQuantity
    ) {}
}
