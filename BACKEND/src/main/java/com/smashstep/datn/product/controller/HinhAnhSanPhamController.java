package com.smashstep.datn.product.controller;

import com.smashstep.datn.product.dto.ImageRequest;
import com.smashstep.datn.product.dto.ImageResponse;
import com.smashstep.datn.product.service.HinhAnhSanPhamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"})
@RequiredArgsConstructor
public class HinhAnhSanPhamController {
    private final HinhAnhSanPhamService service;

    @PutMapping("/product-images/{id}")
    public ImageResponse update(@PathVariable Long id, @Valid @RequestBody ImageRequest request) {
        return service.updateImageById(id, request);
    }

    @DeleteMapping("/product-images/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long id) { service.removeImageById(id); }

    @GetMapping("/products/{productId}/images")
    public java.util.List<ImageResponse> images(@PathVariable Long productId) { return service.listImages(productId); }

    @PostMapping("/products/{productId}/images")
    @ResponseStatus(HttpStatus.CREATED)
    public ImageResponse addImage(@PathVariable Long productId, @Valid @RequestBody ImageRequest request) {
        return service.addImage(productId, request);
    }

    @PutMapping("/products/{productId}/images/{imageId}")
    public ImageResponse updateImage(@PathVariable Long productId, @PathVariable Long imageId,
            @Valid @RequestBody ImageRequest request) { return service.updateImage(productId, imageId, request); }

    @DeleteMapping("/products/{productId}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeImage(@PathVariable Long productId, @PathVariable Long imageId) { service.removeImage(productId, imageId); }
}
