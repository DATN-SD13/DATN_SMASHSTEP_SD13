package com.smashstep.datn.product.controller;

import com.smashstep.datn.product.dto.ProductDtos.*;
import com.smashstep.datn.product.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/product-details")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"})
@RequiredArgsConstructor
public class ProductVariantController {
    private final VariantService service;

    @GetMapping
    public PageResponse<VariantResponse> list(@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="10") int size, @RequestParam(required=false) String keyword,
            @RequestParam(required=false) Integer status, @RequestParam(required=false) Long productId,
            @RequestParam(required=false) Long colorId, @RequestParam(required=false) Long sizeId) {
        return service.list(page, size, keyword, status, productId, colorId, sizeId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VariantResponse create(@Valid @RequestBody VariantRequest request) { return service.create(request); }

    @PutMapping("/{id}")
    public VariantResponse update(@PathVariable Long id, @Valid @RequestBody VariantRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public VariantResponse status(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return service.changeStatus(id, request.trangThai());
    }
}

