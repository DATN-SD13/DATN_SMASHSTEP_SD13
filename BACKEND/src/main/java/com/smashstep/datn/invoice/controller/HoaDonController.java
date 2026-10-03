package com.smashstep.datn.invoice.controller;

import com.smashstep.datn.common.response.ApiResponse;
import com.smashstep.datn.common.response.PageResponse;
import com.smashstep.datn.invoice.dto.CapNhatTrangThaiHoaDonDto;
import com.smashstep.datn.invoice.dto.HoaDonDetailDto;
import com.smashstep.datn.invoice.dto.HoaDonListDto;
import com.smashstep.datn.invoice.dto.LichSuHoaDonDto;
import com.smashstep.datn.invoice.service.HoaDonService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/hoa-don")
@RequiredArgsConstructor
public class HoaDonController {
    private final HoaDonService hoaDonService;

    @GetMapping
    public ApiResponse<PageResponse<HoaDonListDto>> danhSach(
            @RequestParam(required = false) String ma,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) String loaiDon,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<HoaDonListDto> data = hoaDonService.timKiem(
                ma, tuNgay, denNgay, trangThai, loaiDon, page, size
        );
        return ApiResponse.ok("Lấy danh sách hóa đơn thành công", data);
    }

    @GetMapping("/{ma}")
    public ApiResponse<HoaDonDetailDto> chiTiet(@PathVariable String ma) {
        return ApiResponse.ok("Lấy chi tiết hóa đơn thành công", hoaDonService.chiTiet(ma));
    }

    @GetMapping("/{ma}/lich-su")
    public ApiResponse<List<LichSuHoaDonDto>> lichSu(@PathVariable String ma) {
        return ApiResponse.ok("Lấy lịch sử hóa đơn thành công", hoaDonService.lichSu(ma));
    }

    @PutMapping("/{ma}/trang-thai")
    public ApiResponse<HoaDonDetailDto> capNhatTrangThai(
            @PathVariable String ma,
            @RequestBody CapNhatTrangThaiHoaDonDto request) {
        return ApiResponse.ok(
                "Cập nhật trạng thái hóa đơn thành công",
                hoaDonService.capNhatTrangThai(ma, request)
        );
    }
}
