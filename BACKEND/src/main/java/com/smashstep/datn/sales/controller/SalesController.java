package com.smashstep.datn.sales.controller;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.common.response.ApiResponse;
import com.smashstep.datn.common.response.PageResponse;
import com.smashstep.datn.invoice.rules.PosDraftRules;
import com.smashstep.datn.sales.dto.PendingSaleRequest;
import com.smashstep.datn.sales.dto.SalesRequest;
import com.smashstep.datn.sales.dto.SalesResponse.*;
import com.smashstep.datn.sales.service.PendingSaleService;
import com.smashstep.datn.sales.service.PendingSaleService.Draft;
import com.smashstep.datn.sales.service.PosActor;
import com.smashstep.datn.sales.service.SalesService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@RestController
@RequestMapping("/api/ban-hang")
@RequiredArgsConstructor
public class SalesController {
    private final SalesService salesService;
    private final PendingSaleService hoaDonChoService;

    @GetMapping("/hoa-don-cho")
    public ApiResponse<List<Draft>> hoaDonCho() {
        return ApiResponse.ok(hoaDonChoService.danhSachHoaDonCho());
    }

    @GetMapping("/hoa-don-cho/{id}")
    public ApiResponse<Draft> chiTietHoaDonCho(@PathVariable Long id) {
        return ApiResponse.ok(hoaDonChoService.layHoaDonCho(id));
    }

    @PostMapping("/hoa-don-cho")
    public ApiResponse<Draft> taoHoaDonCho() {
        return ApiResponse.ok(hoaDonChoService.taoHoaDonCho());
    }

    @PutMapping("/hoa-don-cho/{id}")
    public ApiResponse<Draft> capNhatHoaDonCho(
            @PathVariable Long id, @Valid @RequestBody PendingSaleRequest yeuCau) {
        return ApiResponse.ok(hoaDonChoService.capNhatHoaDonCho(id, yeuCau));
    }

    @GetMapping("/san-pham")
    public ApiResponse<PageResponse<CatalogItem>> danhSachSanPhamBan(
            @RequestParam(defaultValue = "0") int trang,
            @RequestParam(defaultValue = "20") int kichThuocTrang,
            @RequestParam(defaultValue = "") String tuKhoa,
            @RequestParam(defaultValue = "") String mauSac,
            @RequestParam(defaultValue = "") String kichThuoc,
            @RequestParam(required = false) Long idDanhMuc,
            @RequestParam(required = false) Long idThuongHieu) {
        return ApiResponse.ok(
                salesService.danhSachSanPhamBan(
                        trang, kichThuocTrang, tuKhoa, mauSac, kichThuoc, idDanhMuc, idThuongHieu));
    }

    @GetMapping("/san-pham/{id}")
    public ApiResponse<CatalogItem> chiTietSanPhamBan(@PathVariable Long id) {
        return ApiResponse.ok(salesService.chiTietSanPhamBan(id));
    }

    @GetMapping("/phuong-thuc-thanh-toan")
    public ApiResponse<List<PaymentMethod>> danhSachPhuongThucThanhToan() {
        return ApiResponse.ok(salesService.danhSachPhuongThucThanhToan());
    }

    @PostMapping("/tinh-tien")
    public ApiResponse<Quote> tinhTien(@Valid @RequestBody SalesRequest yeuCau) {
        yeuCau.setIdNhanVien(PosActor.idNhanVien());
        return ApiResponse.ok(salesService.tinhTien(yeuCau));
    }

    @PostMapping("/thanh-toan")
    public ApiResponse<Receipt> thanhToan(@Valid @RequestBody SalesRequest yeuCau) {
        yeuCau.setIdNhanVien(PosActor.idNhanVien());
        String ghiChu = yeuCau.getGhiChu();
        if (yeuCau.getIdHoaDon() != null && ghiChu != null && ghiChu.length() > 980)
            throw AppException.badRequest("Ghi chú tối đa 980 ký tự");
        if (ghiChu != null
                && (ghiChu.startsWith(PosDraftRules.PREFIX)
                || ghiChu.startsWith(PosDraftRules.DELIVERY_PREFIX))) {
            throw AppException.badRequest("Ghi chú chứa tiền tố dành riêng cho hóa đơn chờ");
        }
        return ApiResponse.ok("Thanh toán thành công", salesService.thanhToan(yeuCau));
    }
}
