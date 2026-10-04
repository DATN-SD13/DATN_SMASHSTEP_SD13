package com.smashstep.datn.product.controller;

import com.smashstep.datn.product.dto.ProductDtos.*;
import com.smashstep.datn.product.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"})
@RequiredArgsConstructor
public class ProductController {
    private final ProductService service;
    private final VariantService variants;

    @GetMapping
    public PageResponse<ProductResponse> list(@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="10") int size, @RequestParam(required=false) String keyword,
            @RequestParam(required=false) Integer status, @RequestParam(required=false) Long categoryId,
            @RequestParam(required=false) Long brandId, @RequestParam(required=false) Long materialId,
            @RequestParam(required=false) Long styleId, @RequestParam(required=false) Long collarId,
            @RequestParam(required=false) Long originId) {
        return service.list(page, size, keyword, status, categoryId, brandId, materialId, styleId, collarId, originId);
    }

    @GetMapping("/{id}")
    public ProductDetail detail(@PathVariable Long id) { return service.detail(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request) { return service.create(request); }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public ProductResponse status(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return service.changeStatus(id, request.trangThai());
    }

    @PostMapping("/{id}/variants")
    @ResponseStatus(HttpStatus.CREATED)
    public java.util.List<VariantResponse> batch(@PathVariable Long id, @Valid @RequestBody VariantBatchRequest request) {
        return variants.createBatch(id, request.variants());
    }

    @PostMapping("/{id}/images")
    @ResponseStatus(HttpStatus.CREATED)
    public ImageResponse addImage(@PathVariable Long id, @Valid @RequestBody ImageRequest request) {
        return service.addImage(id, request);
    }

    @PutMapping("/{id}/images/{imageId}")
    public ImageResponse updateImage(@PathVariable Long id, @PathVariable Long imageId,
            @Valid @RequestBody ImageRequest request) { return service.updateImage(id, imageId, request); }

    @DeleteMapping("/{id}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeImage(@PathVariable Long id, @PathVariable Long imageId) { service.removeImage(id, imageId); }
}

