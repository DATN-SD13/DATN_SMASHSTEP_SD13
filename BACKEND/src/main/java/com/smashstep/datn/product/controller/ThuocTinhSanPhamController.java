package com.smashstep.datn.product.controller;

import com.smashstep.datn.common.response.PageResponse;
import com.smashstep.datn.product.dto.DuLieuSanPham.*;
import com.smashstep.datn.product.service.ThuocTinhSanPhamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product-attributes")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"})
@RequiredArgsConstructor
public class ThuocTinhSanPhamController {
    private final ThuocTinhSanPhamService thuocTinhService;

    @GetMapping("/options")
    public Map<String, List<ThuocTinhResponse>> layLuaChon() {
        return thuocTinhService.layLuaChon();
    }

    @GetMapping("/{type}")
    public PageResponse<ThuocTinhResponse> layDanhSach(@PathVariable("type") String loai,
            @RequestParam(value = "page", defaultValue = "0") int trang,
            @RequestParam(value = "size", defaultValue = "10") int kichThuocTrang,
            @RequestParam(value = "keyword", required = false) String tuKhoa,
            @RequestParam(value = "status", required = false) Integer trangThai) {
        return thuocTinhService.layDanhSach(loai, trang, kichThuocTrang, tuKhoa, trangThai);
    }

    @GetMapping("/{type}/{id}")
    public ThuocTinhResponse layChiTiet(@PathVariable("type") String loai, @PathVariable Long id) {
        return thuocTinhService.layChiTiet(loai, id);
    }

    @PostMapping("/{type}")
    @ResponseStatus(HttpStatus.CREATED)
    public ThuocTinhResponse themThuocTinh(@PathVariable("type") String loai,
            @Valid @RequestBody ThuocTinhRequest yeuCau) {
        return thuocTinhService.luuThuocTinh(loai, null, yeuCau);
    }

    @PutMapping("/{type}/{id}")
    public ThuocTinhResponse suaThuocTinh(@PathVariable("type") String loai,
            @PathVariable Long id, @Valid @RequestBody ThuocTinhRequest yeuCau) {
        return thuocTinhService.luuThuocTinh(loai, id, yeuCau);
    }

    @PatchMapping("/{type}/{id}/status")
    public ThuocTinhResponse doiTrangThai(@PathVariable("type") String loai,
            @PathVariable Long id, @Valid @RequestBody TrangThaiRequest yeuCau) {
        return thuocTinhService.doiTrangThai(loai, id, yeuCau.getTrangThai());
    }
}
