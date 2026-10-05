package com.smashstep.datn.promotion.controller;

import com.smashstep.datn.common.response.ApiResponse;
import com.smashstep.datn.common.response.PageResponse;
import com.smashstep.datn.promotion.response.PhieuGiamGiaResponse;
import com.smashstep.datn.promotion.service.PhieuGiamGiaService;
import com.smashstep.datn.promotion.response.PhieuGiamGiaRequest;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/phieu-giam-gia")
@RequiredArgsConstructor
public class PhieuGiamGiaController {

    private final PhieuGiamGiaService phieuGiamGiaService;

    @GetMapping
    public ApiResponse<PageResponse<PhieuGiamGiaResponse>> getAll(
            @RequestParam(required = false)
            String ma,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate tuNgay,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate denNgay,

            @RequestParam(required = false)
            Integer trangThai,

            @RequestParam(defaultValue = "1")
            int page,

            @RequestParam(defaultValue = "5")
            int size
    ) {

        PageResponse<PhieuGiamGiaResponse> data =
                phieuGiamGiaService.getAll(
                        ma,
                        tuNgay,
                        denNgay,
                        trangThai,
                        page,
                        size
                );

        return ApiResponse.ok(data);
    }

    @GetMapping("/{id}")
    public ApiResponse<PhieuGiamGiaResponse> getById(
            @PathVariable Long id
    ) {

        return ApiResponse.ok(
                phieuGiamGiaService.getById(id)
        );
    }

    @PostMapping
    public ApiResponse<PhieuGiamGiaResponse> create(
            @RequestBody PhieuGiamGiaRequest request
    ) {

        return ApiResponse.ok(
                phieuGiamGiaService.create(request)
        );
    }

    @PutMapping("/{id}")
    public ApiResponse<PhieuGiamGiaResponse> update(
            @PathVariable Long id,
            @RequestBody PhieuGiamGiaRequest request
    ) {

        return ApiResponse.ok(
                phieuGiamGiaService.update(id, request)
        );
    }
// xoa mem
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deactivate(
            @PathVariable Long id
    ) {

        phieuGiamGiaService.deactivate(id);

        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}/kich-hoat")
    public ApiResponse<Void> activate(@PathVariable Long id) {

        phieuGiamGiaService.activate(id);

        return ApiResponse.ok(null);
    }
    @DeleteMapping("/{id}/xoa")
    public ApiResponse<Void> delete(@PathVariable Long id) {

        phieuGiamGiaService.delete(id);

        return ApiResponse.ok(null);
    }
}