package com.smashstep.datn.sales.controller;

import com.smashstep.datn.common.response.ApiResponse;
import com.smashstep.datn.sales.dto.SalesDelivery;
import com.smashstep.datn.sales.service.DeliveryRules;
import com.smashstep.datn.sales.service.GhnPhiVanChuyenService;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/ban-hang/van-chuyen")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@RequiredArgsConstructor
public class PhiVanChuyenController {
    private final GhnPhiVanChuyenService service;
    private final EntityManager entityManager;
    public record YeuCau(@Valid @NotNull SalesDelivery giaoHang, @Positive Long idKhachHang,
                         @Min(1) @Max(500000) int soLuong, @Positive Integer idTinhThanh,
                         @Size(max=20) String maPhuongXa) {}
    @GetMapping("/tinh-thanh") public ApiResponse<List<GhnPhiVanChuyenService.Location>> tinhThanh() { return ApiResponse.ok(service.tinhThanh()); }
    @GetMapping("/phuong-xa") public ApiResponse<List<GhnPhiVanChuyenService.Location>> phuongXa(@RequestParam int idTinhThanh) { return ApiResponse.ok(service.phuongXa(idTinhThanh)); }
    @PostMapping("/tinh-phi") public ApiResponse<GhnPhiVanChuyenService.Fee> tinhPhi(@Valid @RequestBody YeuCau request) {
        var address = DeliveryRules.chuanHoa(entityManager, request.idKhachHang(), request.giaoHang());
        return ApiResponse.ok(service.tinhPhi(address, request.soLuong(), request.idTinhThanh(), request.maPhuongXa()));
    }
}
