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

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SalesController {
    private final SalesService service;

    @GetMapping("/catalog")
    public ApiResponse<PageResponse<CatalogItem>> catalog(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "") String keyword) {
        return ApiResponse.ok(service.catalog(page, size, keyword));
    }
    @GetMapping("/payment-methods")
    public ApiResponse<List<PaymentMethod>> paymentMethods() { return ApiResponse.ok(service.paymentMethods()); }
    @GetMapping("/catalog/{id}")
    public ApiResponse<CatalogItem> catalogItem(@PathVariable Long id) { return ApiResponse.ok(service.catalogItem(id)); }
    @PostMapping("/quote")
    public ApiResponse<Quote> quote(@Valid @RequestBody SalesRequest request) {
        return ApiResponse.ok(service.quote(request));
    }
    @PostMapping("/checkout")
    public ApiResponse<Receipt> checkout(@Valid @RequestBody SalesRequest request) {
        return ApiResponse.ok("Thanh toán thành công", service.checkout(request));
    }
}
