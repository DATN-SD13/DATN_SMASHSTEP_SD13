package com.smashstep.datn.product.controller;

import com.smashstep.datn.product.dto.ProductDtos.*;
import com.smashstep.datn.product.dto.*;
import com.smashstep.datn.product.service.SanPhamChiTietService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"})
@RequiredArgsConstructor
public class SanPhamChiTietController {
    private final SanPhamChiTietService service;

    @GetMapping("/product-details")
    public PageResponse<VariantResponse> list(@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="10") int size, @RequestParam(required=false) String keyword,
            @RequestParam(required=false) Integer status, @RequestParam(required=false) Long productId,
            @RequestParam(required=false) Long colorId, @RequestParam(required=false) Long sizeId,
            @RequestParam(required=false) Boolean active) {
        return service.getVariants(page, size, keyword, status, productId, colorId, sizeId, active);
    }

    @GetMapping("/product-details/{id}")
    public VariantResponse detail(@PathVariable Long id) { return service.getVariantById(id); }

    @PostMapping("/product-details")
    @ResponseStatus(HttpStatus.CREATED)
    public VariantResponse create(@Validated(StandaloneVariant.class) @RequestBody VariantCreateRequest request) {
        return service.createVariant(request);
    }

    @PutMapping("/product-details/{id}")
    public VariantResponse update(@PathVariable Long id, @Valid @RequestBody VariantUpdateRequest request) {
        return service.updateVariant(id, request);
    }

    @PatchMapping("/product-details/{id}/status")
    public VariantResponse status(@PathVariable Long id, @Valid @RequestBody VariantStatusRequest request) {
        return service.updateStatus(id, request.getTrangThai());
    }

    @GetMapping("/products/{productId}/variants")
    public java.util.List<VariantResponse> variants(@PathVariable Long productId) {
        return service.getVariantsByProduct(productId);
    }

    @PostMapping("/products/{productId}/variants")
    @ResponseStatus(HttpStatus.CREATED)
    public Object createForProduct(@PathVariable Long productId,
            @Valid @RequestBody VariantSubmissionRequest request) {
        return request.isBatch()
                ? service.createBatch(productId, request.getVariants())
                : service.createVariant(productId, request.getVariants().get(0));
    }
}
