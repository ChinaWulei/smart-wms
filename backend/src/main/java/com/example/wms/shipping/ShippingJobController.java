package com.example.wms.shipping;

import com.example.wms.common.ApiResponse;
import com.example.wms.shipping.ShippingJobDtos.AddOrdersRequest;
import com.example.wms.shipping.ShippingJobDtos.CreateShippingJobRequest;
import com.example.wms.shipping.ShippingJobDtos.UpdateShippingJobRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shipping-jobs")
public class ShippingJobController {
    private final ShippingJobService shippingJobService;

    public ShippingJobController(ShippingJobService shippingJobService) {
        this.shippingJobService = shippingJobService;
    }

    @PostMapping
    public ApiResponse<ShippingJob> create(@Valid @RequestBody CreateShippingJobRequest request) {
        return ApiResponse.ok(shippingJobService.create(request));
    }

    @GetMapping
    public ApiResponse<List<ShippingJob>> list(@RequestParam(required = false) Long warehouseId) {
        return ApiResponse.ok(shippingJobService.list(warehouseId));
    }

    @GetMapping("/{id}")
    public ApiResponse<ShippingJob> get(@PathVariable Long id) {
        return ApiResponse.ok(shippingJobService.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<ShippingJob> update(@PathVariable Long id,
                                           @Valid @RequestBody UpdateShippingJobRequest request) {
        return ApiResponse.ok(shippingJobService.update(id, request));
    }

    @PostMapping("/{id}/orders")
    public ApiResponse<ShippingJob> addOrders(@PathVariable Long id, @Valid @RequestBody AddOrdersRequest request) {
        return ApiResponse.ok(shippingJobService.addOrders(id, request.outboundOrderIds()));
    }

    @DeleteMapping("/{id}/orders/{orderId}")
    public ApiResponse<ShippingJob> removeOrder(@PathVariable Long id, @PathVariable Long orderId) {
        return ApiResponse.ok(shippingJobService.removeOrder(id, orderId));
    }

    @PostMapping("/{id}/schedule")
    public ApiResponse<ShippingJob> schedule(@PathVariable Long id) {
        return ApiResponse.ok(shippingJobService.schedule(id));
    }

    @PostMapping("/{id}/ship")
    public ApiResponse<ShippingJob> ship(@PathVariable Long id) {
        return ApiResponse.ok(shippingJobService.ship(id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<ShippingJob> cancel(@PathVariable Long id) {
        return ApiResponse.ok(shippingJobService.cancel(id));
    }
}
