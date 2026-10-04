package com.smashstep.datn.product.controller;

import com.smashstep.datn.product.dto.ProductDtos.ImageRequest;
import com.smashstep.datn.product.dto.ProductDtos.ImageResponse;
import com.smashstep.datn.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product-images")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"})
@RequiredArgsConstructor
public class ProductImageController {
    private final ProductService service;

    @PutMapping("/{id}")
    public ImageResponse update(@PathVariable Long id, @Valid @RequestBody ImageRequest request) {
        return service.updateImageById(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long id) { service.removeImageById(id); }
}
