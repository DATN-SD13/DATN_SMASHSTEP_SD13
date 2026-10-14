package com.smashstep.datn.schedule.controller;

import com.smashstep.datn.common.response.ApiResponse;
import com.smashstep.datn.common.response.PageResponse;
import com.smashstep.datn.schedule.dto.CapNhatTrangThaiRequest;
import com.smashstep.datn.schedule.dto.LichLamViecRequest;
import com.smashstep.datn.schedule.dto.LichLamViecResponse;
import com.smashstep.datn.schedule.service.LichLamViecService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/lich-lam-viec")
@RequiredArgsConstructor
public class LichLamViecController {

    private final LichLamViecService lichLamViecService;

    /** Danh sách có phân trang + filter */
    @GetMapping
    public ApiResponse<PageResponse<LichLamViecResponse>> getAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
            @RequestParam(required = false) Long caId,
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok("Lấy lịch làm việc thành công",
                lichLamViecService.getAll(tuNgay, denNgay, caId, tuKhoa, page, size));
    }

    /** Lấy toàn bộ lịch trong 1 tuần (cho calendar view) */
    @GetMapping("/tuan")
    public ApiResponse<List<LichLamViecResponse>> getByWeek(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay
    ) {
        return ApiResponse.ok("Lấy lịch tuần thành công", lichLamViecService.getByWeek(tuNgay));
    }

    /** Phân ca — có thể phân cho nhiều nhân viên cùng lúc */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<LichLamViecResponse>> create(
            @Valid @RequestBody LichLamViecRequest request
    ) {
        return ApiResponse.ok("Phân ca thành công", lichLamViecService.create(request));
    }

    /** Cập nhật trạng thái (xác nhận / vắng / hoàn thành) */
    @PatchMapping("/{id}/trang-thai")
    public ApiResponse<LichLamViecResponse> updateTrangThai(
            @PathVariable Long id,
            @Valid @RequestBody CapNhatTrangThaiRequest request
    ) {
        return ApiResponse.ok("Cập nhật trạng thái thành công",
                lichLamViecService.updateTrangThai(id, request));
    }

    /** Xóa lịch */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        lichLamViecService.delete(id);
        return ApiResponse.<Void>ok("Xóa lịch làm việc thành công");
    }
}
