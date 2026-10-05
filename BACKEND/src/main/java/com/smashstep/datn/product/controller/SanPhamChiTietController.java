package com.smashstep.datn.product.controller;

import com.smashstep.datn.common.response.PageResponse;
import com.smashstep.datn.product.dto.*;
import com.smashstep.datn.product.dto.DuLieuSanPham.BienTheDocLap;
import com.smashstep.datn.product.dto.DuLieuSanPham.DanhSachBienTheRequest;
import com.smashstep.datn.product.service.SanPhamChiTietService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"})
@RequiredArgsConstructor
public class SanPhamChiTietController {
    private final SanPhamChiTietService sanPhamChiTietService;

    @GetMapping("/product-details")
    public PageResponse<BienTheResponse> layDanhSach(
            @RequestParam(value = "page", defaultValue = "0") int trang,
            @RequestParam(value = "size", defaultValue = "10") int kichThuocTrang,
            @RequestParam(value = "keyword", required = false) String tuKhoa,
            @RequestParam(value = "status", required = false) Integer trangThai,
            @RequestParam(value = "productId", required = false) Long sanPhamId,
            @RequestParam(value = "colorId", required = false) Long mauSacId,
            @RequestParam(value = "sizeId", required = false) Long kichThuocId,
            @RequestParam(value = "active", required = false) Boolean kichHoat) {
        return sanPhamChiTietService.layDanhSach(trang, kichThuocTrang, tuKhoa,
                trangThai, sanPhamId, mauSacId, kichThuocId, kichHoat);
    }

    @GetMapping("/product-details/{id}")
    public BienTheResponse layChiTiet(@PathVariable Long id) {
        return sanPhamChiTietService.layChiTiet(id);
    }

    @PostMapping("/product-details")
    @ResponseStatus(HttpStatus.CREATED)
    public BienTheResponse themBienThe(
            @Validated(BienTheDocLap.class) @RequestBody SanPhamChiTietThemRequest yeuCau) {
        return sanPhamChiTietService.themSanPhamChiTiet(yeuCau);
    }

    @PutMapping("/product-details/{id}")
    public BienTheResponse suaBienThe(@PathVariable Long id,
            @Valid @RequestBody SanPhamChiTietSuaRequest yeuCau) {
        return sanPhamChiTietService.suaSanPhamChiTiet(id, yeuCau);
    }

    @PatchMapping("/product-details/{id}/status")
    public BienTheResponse doiTrangThai(@PathVariable Long id,
            @Valid @RequestBody SanPhamChiTietTrangThaiRequest yeuCau) {
        return sanPhamChiTietService.doiTrangThai(id, yeuCau.getTrangThai());
    }

    @GetMapping("/products/{productId}/variants")
    public List<BienTheResponse> layTheoSanPham(@PathVariable("productId") Long sanPhamId) {
        return sanPhamChiTietService.layTheoSanPham(sanPhamId);
    }

    @PostMapping("/products/{productId}/variants")
    @ResponseStatus(HttpStatus.CREATED)
    public Object themTheoSanPham(@PathVariable("productId") Long sanPhamId,
            @Valid @RequestBody DanhSachBienTheRequest yeuCau) {
        if (yeuCau.isHangLoat()) {
            return sanPhamChiTietService.themDanhSachBienThe(sanPhamId, yeuCau.getVariants());
        }
        return sanPhamChiTietService.themSanPhamChiTiet(sanPhamId, yeuCau.getVariants().get(0));
    }
}
