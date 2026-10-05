package com.smashstep.datn.promotion.controller;

import com.smashstep.datn.common.response.ApiResponse;
import com.smashstep.datn.common.response.PageResponse;
import com.smashstep.datn.promotion.dto.DotGiamGiaRequest;
import com.smashstep.datn.promotion.dto.DotGiamGiaResponse;
import com.smashstep.datn.promotion.entity.DotGiamGia;
import com.smashstep.datn.promotion.service.DotGiamGiaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.smashstep.datn.promotion.dto.DotGiamGiaDetailResponse;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/dot-giam-gia")
@RequiredArgsConstructor
public class DotGiamGiaController {

    private final DotGiamGiaService dotGiamGiaService;

    @PostMapping
    public ResponseEntity<ApiResponse<DotGiamGia>> create(
            @RequestBody DotGiamGiaRequest request
    ) {
        DotGiamGia data = dotGiamGiaService.create(request);

        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Tạo đợt giảm giá thành công",
                        data
                )
        );
    }
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DotGiamGia>> update(
            @PathVariable Long id,
            @RequestBody DotGiamGiaRequest request
    ) {
        DotGiamGia data =
                dotGiamGiaService.update(id, request);

        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Cập nhật đợt giảm giá thành công",
                        data
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id
    ) {
        dotGiamGiaService.delete(id);

        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Ngừng hoạt động đợt giảm giá thành công"
                )
        );
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DotGiamGiaResponse>>> search(
            @RequestParam(required = false) String ma,
            @RequestParam(required = false) String ten,
            @RequestParam(required = false) Integer trangThai,
            @RequestParam(required = false) LocalDateTime tuNgay,
            @RequestParam(required = false) LocalDateTime denNgay,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<DotGiamGiaResponse> data =
                dotGiamGiaService.search(
                        ma,
                        ten,
                        trangThai,
                        tuNgay,
                        denNgay,
                        page,
                        size
                );

        return ResponseEntity.ok(
                ApiResponse.ok(data)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DotGiamGiaDetailResponse>> getDetail(
            @PathVariable Long id
    ) {
        DotGiamGiaDetailResponse data =
                dotGiamGiaService.getDetail(id);

        return ResponseEntity.ok(
                ApiResponse.ok(data)
        );
    }
}