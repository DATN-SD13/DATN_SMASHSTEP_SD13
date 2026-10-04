package com.smashstep.datn.product.controller;

import com.smashstep.datn.product.dto.ProductDtos.*;
import com.smashstep.datn.product.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/product-attributes")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"})
@RequiredArgsConstructor
public class ProductAttributeController {
    private final ProductAttributeService service;

    @GetMapping("/options")
    public java.util.Map<String, java.util.List<AttributeResponse>> options() { return service.options(); }

    @GetMapping("/{type}")
    public PageResponse<AttributeResponse> list(@PathVariable String type,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="10") int size,
            @RequestParam(required=false) String keyword, @RequestParam(required=false) Integer status) {
        return service.list(type, page, size, keyword, status);
    }

    @PostMapping("/{type}")
    @ResponseStatus(HttpStatus.CREATED)
    public AttributeResponse create(@PathVariable String type, @Valid @RequestBody AttributeRequest request) {
        return service.save(type, null, request);
    }

    @PutMapping("/{type}/{id}")
    public AttributeResponse update(@PathVariable String type, @PathVariable Long id,
            @Valid @RequestBody AttributeRequest request) { return service.save(type, id, request); }

    @PatchMapping("/{type}/{id}/status")
    public AttributeResponse status(@PathVariable String type, @PathVariable Long id,
            @Valid @RequestBody StatusRequest request) { return service.changeStatus(type, id, request.trangThai()); }
}

