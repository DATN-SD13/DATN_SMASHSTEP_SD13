package com.smashstep.datn.schedule.controller;

import com.smashstep.datn.common.response.ApiResponse;
import com.smashstep.datn.schedule.dto.CaLamViecRequest;
import com.smashstep.datn.schedule.dto.CaLamViecResponse;
import com.smashstep.datn.schedule.service.CaLamViecService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/ca-lam-viec")
@RequiredArgsConstructor
public class CaLamViecController {

    private final CaLamViecService caLamViecService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ApiResponse<List<CaLamViecResponse>> getAll() {
        return ApiResponse.ok("Lấy danh sách ca làm việc thành công", caLamViecService.getAll());
    }

    // Cho phép mọi user đã đăng nhập — dùng cho panel ca cửa hàng ở header
    @GetMapping("/active")
    public ApiResponse<List<CaLamViecResponse>> getActive() {
        return ApiResponse.ok("Lấy danh sách ca đang hoạt động thành công", caLamViecService.getActive());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ApiResponse<CaLamViecResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok("Lấy ca làm việc thành công", caLamViecService.getById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CaLamViecResponse> create(@Valid @RequestBody CaLamViecRequest request) {
        return ApiResponse.ok("Tạo ca làm việc thành công", caLamViecService.create(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ApiResponse<CaLamViecResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CaLamViecRequest request) {
        return ApiResponse.ok("Cập nhật ca làm việc thành công", caLamViecService.update(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/trang-thai")
    public ApiResponse<CaLamViecResponse> toggleTrangThai(@PathVariable Long id) {
        return ApiResponse.ok("Cập nhật trạng thái thành công", caLamViecService.toggleTrangThai(id));
    }
}
