package com.smashstep.datn.employee.controller;

import com.smashstep.datn.common.response.ApiResponse;
import com.smashstep.datn.common.response.PageResponse;
import com.smashstep.datn.employee.dto.NhanVienRequest;
import com.smashstep.datn.employee.dto.NhanVienResponse;
import com.smashstep.datn.employee.dto.TrangThaiNhanVienRequest;
import com.smashstep.datn.employee.dto.VaiTroResponse;
import com.smashstep.datn.employee.service.NhanVienService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/nhan-vien")
@RequiredArgsConstructor
public class NhanVienController {

    private final NhanVienService nhanVienService;

    // =========================================================
    // DANH SÁCH
    // =========================================================
    @GetMapping
    public ApiResponse<PageResponse<NhanVienResponse>> layDanhSach(
            @RequestParam(required = false)
            String tuKhoa,

            @RequestParam(required = false)
            Long vaiTroId,

            @RequestParam(required = false)
            Integer trangThai,

            @RequestParam(defaultValue = "1")
            int page,

            @RequestParam(defaultValue = "5")
            int size
    ) {

        PageResponse<NhanVienResponse> data =
                nhanVienService.layDanhSach(
                        tuKhoa,
                        vaiTroId,
                        trangThai,
                        page,
                        size
                );

        return ApiResponse.ok(
                "Lấy danh sách nhân viên thành công",
                data
        );
    }

    // =========================================================
    // DANH SÁCH VAI TRÒ
    // =========================================================
    @GetMapping("/vai-tro")
    public ApiResponse<List<VaiTroResponse>> layDanhSachVaiTro() {

        return ApiResponse.ok(
                "Lấy danh sách vai trò thành công",
                nhanVienService.layDanhSachVaiTro()
        );
    }

    // =========================================================
    // CHI TIẾT
    // =========================================================
    @GetMapping("/{ma}")
    public ApiResponse<NhanVienResponse> layChiTiet(
            @PathVariable String ma
    ) {

        return ApiResponse.ok(
                "Lấy chi tiết nhân viên thành công",
                nhanVienService.layChiTiet(ma)
        );
    }

    // =========================================================
    // THÊM
    // =========================================================
    @PostMapping
    public ApiResponse<NhanVienResponse> them(
            @Valid
            @RequestBody
            NhanVienRequest request
    ) {

        return ApiResponse.ok(
                "Thêm nhân viên thành công",
                nhanVienService.them(request)
        );
    }

    // =========================================================
    // SỬA
    // =========================================================
    @PutMapping("/{ma}")
    public ApiResponse<NhanVienResponse> sua(
            @PathVariable String ma,

            @Valid
            @RequestBody
            NhanVienRequest request
    ) {

        return ApiResponse.ok(
                "Cập nhật nhân viên thành công",
                nhanVienService.sua(
                        ma,
                        request
                )
        );
    }

    // =========================================================
    // KHÓA / MỞ KHÓA
    // =========================================================
    @PatchMapping("/{ma}/trang-thai")
    public ApiResponse<NhanVienResponse> capNhatTrangThai(
            @PathVariable String ma,

            @Valid
            @RequestBody
            TrangThaiNhanVienRequest request
    ) {

        return ApiResponse.ok(
                "Cập nhật trạng thái nhân viên thành công",
                nhanVienService.capNhatTrangThai(
                        ma,
                        request
                )
        );
    }
}