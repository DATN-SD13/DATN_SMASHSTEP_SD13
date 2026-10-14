package com.smashstep.datn.sales.controller;

import com.smashstep.datn.common.response.ApiResponse;
import com.smashstep.datn.common.response.PageResponse;
import com.smashstep.datn.sales.dto.SalesRequest;
import com.smashstep.datn.sales.dto.SalesResponse.*;
import com.smashstep.datn.sales.service.SalesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SalesController {
    private final SalesService service;
    private final com.smashstep.datn.sales.service.PendingSaleService pending;

    @GetMapping("/pending")
    public ApiResponse<java.util.List<com.smashstep.datn.sales.service.PendingSaleService.Draft>> pending() {
        return ApiResponse.ok(pending.list());
    }
    @GetMapping("/pending/{id}")
    public ApiResponse<com.smashstep.datn.sales.service.PendingSaleService.Draft> pending(@PathVariable Long id) {
        return ApiResponse.ok(pending.get(id));
    }
    @PostMapping("/pending")
    public ApiResponse<com.smashstep.datn.sales.service.PendingSaleService.Draft> createPending() {
        return ApiResponse.ok(pending.create());
    }
    @PutMapping("/pending/{id}")
    public ApiResponse<com.smashstep.datn.sales.service.PendingSaleService.Draft> updatePending(@PathVariable Long id,
                                                                                                @Valid @RequestBody com.smashstep.datn.sales.dto.PendingSaleRequest request) {
        return ApiResponse.ok(pending.update(id, request));
    }

    @GetMapping("/catalog")
    public ApiResponse<PageResponse<CatalogItem>> catalog(@RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "") String keyword,
                                                          @RequestParam(defaultValue = "") String color, @RequestParam(defaultValue = "") String variantSize,
                                                          @RequestParam(required = false) Long categoryId, @RequestParam(required = false) Long brandId) {
        return ApiResponse.ok(service.catalog(page, size, keyword, color, variantSize, categoryId, brandId));
    }
    @GetMapping("/payment-methods")
    public ApiResponse<List<PaymentMethod>> paymentMethods() { return ApiResponse.ok(service.paymentMethods()); }
    @GetMapping("/catalog/{id}")
    public ApiResponse<CatalogItem> catalogItem(@PathVariable Long id) { return ApiResponse.ok(service.catalogItem(id)); }
    @PostMapping("/quote")
    public ApiResponse<Quote> quote(@Valid @RequestBody SalesRequest request) {
        request.setEmployeeId(com.smashstep.datn.sales.service.PosActor.employeeId());
        return ApiResponse.ok(service.quote(request));
    }
    @PostMapping("/checkout")
    public ApiResponse<Receipt> checkout(@Valid @RequestBody SalesRequest request) {
        request.setEmployeeId(com.smashstep.datn.sales.service.PosActor.employeeId());
        if (request.getNote() != null && (request.getNote().startsWith(com.smashstep.datn.invoice.rules.PosDraftRules.PREFIX)
                || request.getNote().startsWith(com.smashstep.datn.invoice.rules.PosDraftRules.DELIVERY_PREFIX)))
            throw com.smashstep.datn.common.exception.AppException.badRequest("Ghi chú chứa tiền tố dành riêng cho hóa đơn chờ");
        return ApiResponse.ok("Thanh toán thành công", service.checkout(request));
    }
}
