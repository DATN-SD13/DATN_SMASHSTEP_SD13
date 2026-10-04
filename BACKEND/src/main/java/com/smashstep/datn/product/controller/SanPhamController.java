package com.smashstep.datn.product.controller;

import com.smashstep.datn.product.dto.ProductDtos.PageResponse;
import com.smashstep.datn.product.dto.*;
import com.smashstep.datn.product.service.SanPhamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"})
@RequiredArgsConstructor
public class SanPhamController {
    private final SanPhamService service;

    @GetMapping
    public PageResponse<ProductResponse> list(@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="10") int size, @RequestParam(required=false) String keyword,
            @RequestParam(required=false) Integer status, @RequestParam(required=false) Long categoryId,
            @RequestParam(required=false) Long brandId, @RequestParam(required=false) Long materialId,
            @RequestParam(required=false) Long styleId, @RequestParam(required=false) Long collarId,
            @RequestParam(required=false) Long originId) {
        return service.getProducts(page, size, keyword, status, categoryId, brandId, materialId, styleId, collarId, originId);
    }

    @GetMapping("/{id}")
    public ProductDetailResponse detail(@PathVariable Long id) { return service.getProductById(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductCreateRequest request) { return service.createProduct(request); }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
        return service.updateProduct(id, request);
    }

    @PatchMapping("/{id}/status")
    public ProductResponse status(@PathVariable Long id, @Valid @RequestBody ProductStatusRequest request) {
        return service.updateStatus(id, request.getTrangThai());
    }

}
