package com.smashstep.datn.schedule.controller;

import com.smashstep.datn.common.response.ApiResponse;
import com.smashstep.datn.schedule.dto.CaLamViecRequest;
import com.smashstep.datn.schedule.dto.CaLamViecResponse;
import com.smashstep.datn.schedule.service.CaLamViecService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/ca-lam-viec")
@RequiredArgsConstructor
public class CaLamViecController {

    private final CaLamViecService caLamViecService;

    @GetMapping
    public ApiResponse<List<CaLamViecResponse>> getAll() {
        return ApiResponse.ok("Lấy danh sách ca làm việc thành công", caLamViecService.getAll());
    }

    @GetMapping("/active")
    public ApiResponse<List<CaLamViecResponse>> getActive() {
        return ApiResponse.ok("Lấy danh sách ca đang hoạt động thành công", caLamViecService.getActive());
    }

    @GetMapping("/{id}")
    public ApiResponse<CaLamViecResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok("Lấy ca làm việc thành công", caLamViecService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CaLamViecResponse> create(@Valid @RequestBody CaLamViecRequest request) {
        return ApiResponse.ok("Tạo ca làm việc thành công", caLamViecService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CaLamViecResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CaLamViecRequest request) {
        return ApiResponse.ok("Cập nhật ca làm việc thành công", caLamViecService.update(id, request));
    }

    @PatchMapping("/{id}/trang-thai")
    public ApiResponse<CaLamViecResponse> toggleTrangThai(@PathVariable Long id) {
        return ApiResponse.ok("Cập nhật trạng thái thành công", caLamViecService.toggleTrangThai(id));
    }
}
