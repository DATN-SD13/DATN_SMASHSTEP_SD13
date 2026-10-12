package com.smashstep.datn.product.controller;

import com.smashstep.datn.common.response.PageResponse;
import com.smashstep.datn.product.dto.*;
import com.smashstep.datn.product.service.SanPhamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"})
@RequiredArgsConstructor
public class SanPhamController {
    private final SanPhamService sanPhamService;

    @GetMapping
    public PageResponse<SanPhamResponse> layDanhSach(
            @RequestParam(value = "page", defaultValue = "0") int trang,
            @RequestParam(value = "size", defaultValue = "10") int kichThuocTrang,
            @RequestParam(value = "keyword", required = false) String tuKhoa,
            @RequestParam(value = "status", required = false) Integer trangThai,
            @RequestParam(value = "categoryId", required = false) Long danhMucId,
            @RequestParam(value = "brandId", required = false) Long thuongHieuId,
            @RequestParam(value = "materialId", required = false) Long chatLieuId,
            @RequestParam(value = "styleId", required = false) Long kieuDangId,
            @RequestParam(value = "collarId", required = false) Long coGiayId,
            @RequestParam(value = "originId", required = false) Long xuatXuId) {
        return sanPhamService.layDanhSachSanPham(trang, kichThuocTrang, tuKhoa, trangThai,
                danhMucId, thuongHieuId, chatLieuId, kieuDangId, coGiayId, xuatXuId);
    }

    @GetMapping("/{id}")
    public SanPhamChiTietResponse layChiTiet(@PathVariable Long id) {
        return sanPhamService.layChiTietSanPham(id);
    }

    @PostMapping("/check-duplicate")
    public KiemTraTrungResponse.Product kiemTraTrung(@Valid @RequestBody SanPhamTrungRequest request) {
        return sanPhamService.kiemTraTrung(request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SanPhamResponse themSanPham(@Valid @RequestBody SanPhamThemRequest yeuCau) {
        return sanPhamService.themSanPham(yeuCau);
    }

    @PutMapping("/{id}")
    public SanPhamResponse suaSanPham(@PathVariable Long id, @Valid @RequestBody SanPhamSuaRequest yeuCau) {
        return sanPhamService.suaSanPham(id, yeuCau);
    }

    @PatchMapping("/{id}/status")
    public SanPhamResponse doiTrangThai(@PathVariable Long id, @Valid @RequestBody SanPhamTrangThaiRequest yeuCau) {
        return sanPhamService.doiTrangThai(id, yeuCau.getTrangThai());
    }
}
