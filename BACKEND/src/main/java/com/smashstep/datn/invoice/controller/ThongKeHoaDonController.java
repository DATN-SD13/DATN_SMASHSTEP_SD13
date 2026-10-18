package com.smashstep.datn.invoice.controller;

import com.smashstep.datn.common.response.ApiResponse;
import com.smashstep.datn.invoice.dto.ThongKeHoaDonDto;
import com.smashstep.datn.invoice.service.ThongKeHoaDonService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/thong-ke")
@RequiredArgsConstructor
public class ThongKeHoaDonController {
    private final ThongKeHoaDonService service;

    @GetMapping
    public ApiResponse<ThongKeHoaDonDto> tongHop(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
            @RequestParam(required = false) String ma,
            @RequestParam(required = false) String loaiDon) {
        return ApiResponse.ok("Lấy thống kê hóa đơn thành công", service.tongHop(tuNgay, denNgay, ma, loaiDon));
    }
}
