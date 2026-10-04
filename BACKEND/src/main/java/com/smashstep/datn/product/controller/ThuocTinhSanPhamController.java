package com.smashstep.datn.product.controller;

import com.smashstep.datn.common.response.PageResponse;
import com.smashstep.datn.product.dto.DuLieuSanPham.*;
import com.smashstep.datn.product.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/product-attributes")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"})
@RequiredArgsConstructor
public class ThuocTinhSanPhamController {
    private final ThuocTinhSanPhamService service;

    @GetMapping("/options")
    public java.util.Map<String, java.util.List<ThuocTinhResponse>> options() { return service.options(); }

    @GetMapping("/{type}")
    public PageResponse<ThuocTinhResponse> list(@PathVariable String type,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="10") int size,
            @RequestParam(required=false) String keyword, @RequestParam(required=false) Integer status) {
        return service.list(type, page, size, keyword, status);
    }

    @PostMapping("/{type}")
    @ResponseStatus(HttpStatus.CREATED)
    public ThuocTinhResponse create(@PathVariable String type, @Valid @RequestBody ThuocTinhRequest request) {
        return service.save(type, null, request);
    }

    @GetMapping("/{type}/{id}")
    public ThuocTinhResponse detail(@PathVariable String type, @PathVariable Long id) {
        return service.detail(type, id);
    }

    @PutMapping("/{type}/{id}")
    public ThuocTinhResponse update(@PathVariable String type, @PathVariable Long id,
            @Valid @RequestBody ThuocTinhRequest request) { return service.save(type, id, request); }

    @PatchMapping("/{type}/{id}/status")
    public ThuocTinhResponse status(@PathVariable String type, @PathVariable Long id,
            @Valid @RequestBody TrangThaiRequest request) { return service.changeStatus(type, id, request.getTrangThai()); }
}
