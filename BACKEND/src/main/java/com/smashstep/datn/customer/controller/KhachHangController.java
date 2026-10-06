package com.smashstep.datn.customer.controller;

import com.smashstep.datn.common.response.ApiResponse;
import com.smashstep.datn.common.response.PageResponse;
import com.smashstep.datn.customer.dto.DiaChiKhachHangRequest;
import com.smashstep.datn.customer.dto.DiaChiKhachHangResponse;
import com.smashstep.datn.customer.dto.KhachHangRequest;
import com.smashstep.datn.customer.dto.KhachHangResponse;
import com.smashstep.datn.customer.dto.TrangThaiKhachHangRequest;
import com.smashstep.datn.customer.service.KhachHangService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/khach-hang")
@RequiredArgsConstructor
public class KhachHangController {

    private final KhachHangService khachHangService;

    @GetMapping
    public ApiResponse<PageResponse<KhachHangResponse>>
    getAll(
            @RequestParam(required = false)
            String tuKhoa,

            @RequestParam(required = false)
            Integer trangThai,

            @RequestParam(defaultValue = "1")
            int page,

            @RequestParam(defaultValue = "5")
            int size
    ) {
        return ApiResponse.ok(
                khachHangService.getAll(
                        tuKhoa,
                        trangThai,
                        page,
                        size
                )
        );
    }

    @GetMapping("/{ma}")
    public ApiResponse<KhachHangResponse>
    getByCode(
            @PathVariable("ma")
            String ma
    ) {
        return ApiResponse.ok(
                khachHangService.getByCode(ma)
        );
    }

    @PostMapping
    public ApiResponse<KhachHangResponse>
    create(
            @Valid
            @RequestBody
            KhachHangRequest request
    ) {
        return ApiResponse.ok(
                "Tạo khách hàng thành công",
                khachHangService.create(request)
        );
    }

    @PutMapping("/{ma}")
    public ApiResponse<KhachHangResponse>
    update(
            @PathVariable("ma")
            String ma,

            @Valid
            @RequestBody
            KhachHangRequest request
    ) {
        return ApiResponse.ok(
                "Cập nhật khách hàng thành công",
                khachHangService.update(
                        ma,
                        request
                )
        );
    }

    @PatchMapping("/{ma}/trang-thai")
    public ApiResponse<KhachHangResponse>
    updateStatus(
            @PathVariable("ma")
            String ma,

            @Valid
            @RequestBody
            TrangThaiKhachHangRequest request
    ) {
        return ApiResponse.ok(
                "Cập nhật trạng thái thành công",
                khachHangService.updateStatus(
                        ma,
                        request
                )
        );
    }

    @GetMapping("/{ma}/dia-chi")
    public ApiResponse<List<DiaChiKhachHangResponse>>
    getAddresses(
            @PathVariable("ma")
            String ma
    ) {
        return ApiResponse.ok(
                khachHangService
                        .getAddresses(ma)
        );
    }

    @PostMapping("/{ma}/dia-chi")
    public ApiResponse<DiaChiKhachHangResponse>
    createAddress(
            @PathVariable("ma")
            String ma,

            @Valid
            @RequestBody
            DiaChiKhachHangRequest request
    ) {
        return ApiResponse.ok(
                "Thêm địa chỉ thành công",
                khachHangService.createAddress(
                        ma,
                        request
                )
        );
    }

    @PutMapping("/{ma}/dia-chi/{id}")
    public ApiResponse<DiaChiKhachHangResponse>
    updateAddress(
            @PathVariable("ma")
            String ma,

            @PathVariable("id")
            Long id,

            @Valid
            @RequestBody
            DiaChiKhachHangRequest request
    ) {
        return ApiResponse.ok(
                "Cập nhật địa chỉ thành công",
                khachHangService.updateAddress(
                        ma,
                        id,
                        request
                )
        );
    }

    @PatchMapping(
            "/{ma}/dia-chi/{id}/mac-dinh"
    )
    public ApiResponse<DiaChiKhachHangResponse>
    setDefaultAddress(
            @PathVariable("ma")
            String ma,

            @PathVariable("id")
            Long id
    ) {
        return ApiResponse.ok(
                "Đã đặt địa chỉ mặc định",
                khachHangService
                        .setDefaultAddress(
                                ma,
                                id
                        )
        );
    }

    @PatchMapping(
            "/{ma}/dia-chi/{id}/ngung-hoat-dong"
    )
    public ApiResponse<Void>
    deleteAddress(
            @PathVariable("ma")
            String ma,

            @PathVariable("id")
            Long id
    ) {
        khachHangService.deleteAddress(
                ma,
                id
        );

        return ApiResponse.ok(
                "Đã ngừng sử dụng địa chỉ"
        );
    }
}