package com.smashstep.datn.storefront.controller;

import com.smashstep.datn.common.response.ApiResponse;
import com.smashstep.datn.common.response.PageResponse;
import com.smashstep.datn.storefront.dto.*;
import com.smashstep.datn.storefront.service.StorefrontProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
public class StorefrontProductController {
    private final StorefrontProductService products;

    @GetMapping("/products")
    public ApiResponse<PageResponse<StorefrontProductResponse>> products(@ModelAttribute StorefrontQuery query) {
        return ApiResponse.ok(products.list(query));
    }
    @GetMapping("/products/{id}")
    public ApiResponse<StorefrontProductDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(products.detail(id));
    }
    @GetMapping("/filters")
    public ApiResponse<StorefrontFilterResponse> filters() {
        return ApiResponse.ok(products.filters());
    }
    @GetMapping("/brands")
    public ApiResponse<List<StorefrontFilterResponse.Option>> brands() {
        return ApiResponse.ok(products.brands());
    }
}
