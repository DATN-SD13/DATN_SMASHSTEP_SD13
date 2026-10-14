package com.smashstep.datn.schedule.controller;

import com.smashstep.datn.common.response.ApiResponse;
import com.smashstep.datn.schedule.dto.CaLamViecResponse;
import com.smashstep.datn.schedule.service.CaLamViecService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
