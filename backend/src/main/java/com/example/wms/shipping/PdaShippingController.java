package com.example.wms.shipping;

import com.example.wms.common.ApiResponse;
import com.example.wms.shipping.PdaDtos.PdaShippingJobView;
import com.example.wms.shipping.PdaDtos.ScanRequest;
import com.example.wms.shipping.PdaDtos.ScanResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pda/shipping-jobs")
public class PdaShippingController {
    private final PdaShippingService pdaShippingService;

    public PdaShippingController(PdaShippingService pdaShippingService) {
        this.pdaShippingService = pdaShippingService;
    }

    @GetMapping
    public ApiResponse<List<ShippingJob>> list(@RequestParam(required = false) Long warehouseId) {
        return ApiResponse.ok(pdaShippingService.list(warehouseId));
    }

    @GetMapping("/{jobNo}")
    public ApiResponse<PdaShippingJobView> load(@PathVariable String jobNo) {
        return ApiResponse.ok(pdaShippingService.load(jobNo));
    }

    @PostMapping("/{jobNo}/scan")
    public ApiResponse<ScanResponse> scan(@PathVariable String jobNo, @Valid @RequestBody ScanRequest request) {
        return ApiResponse.ok(pdaShippingService.scan(jobNo, request.code(), request.quantity()));
    }

    @PostMapping("/{jobNo}/complete")
    public ApiResponse<PdaShippingJobView> complete(@PathVariable String jobNo) {
        return ApiResponse.ok(pdaShippingService.complete(jobNo));
    }
}
