package com.smashstep.datn.promotion.controller;

import com.smashstep.datn.promotion.request.DotGiamGiaRequest;
import com.smashstep.datn.promotion.response.DotGiamGiaResponse;
import com.smashstep.datn.promotion.service.DotGiamGiaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.smashstep.datn.promotion.response.SanPhamChiTietGiamGiaResponse;
import com.smashstep.datn.promotion.request.DotGiamGiaStatusRequest;
import java.util.List;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
@RestController
@RequestMapping("/api/dot-giam-gia")
@RequiredArgsConstructor
public class DotGiamGiaController {

    private final DotGiamGiaService dotGiamGiaService;

    @GetMapping("/capabilities")
    public ResponseEntity<Map<String, Object>> getCapabilities() {
        return ResponseEntity.ok(Map.of("success", true, "data",
                Map.of("descriptionSupported", dotGiamGiaService.supportsDescription())));
    }

    /**
     * Danh sách + tìm kiếm + lọc + phân trang
     *
     * VD:
     * GET /api/dot-giam-gia?page=1&size=10
     * GET /api/dot-giam-gia?ma=summer
     * GET /api/dot-giam-gia?trangThai=1
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll(
            @RequestParam(required = false) String ma,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate tuNgay,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate denNgay,

            @RequestParam(required = false)
            Integer trangThai,

            @RequestParam(defaultValue = "1")
            int page,

            @RequestParam(defaultValue = "10")
            int size
    ) {

        Page<DotGiamGiaResponse> result =
                dotGiamGiaService.getAll(
                        ma,
                        tuNgay,
                        denNgay,
                        trangThai,
                        page,
                        size
                );

        // PageResponse tạm thời.
        // Nếu project của Quang đã có class PageResponse chung
        // thì bước sau sẽ thay bằng class chung.
        Map<String, Object> pageData = new LinkedHashMap<>();

        pageData.put("content", result.getContent());
        pageData.put("page", result.getNumber() + 1);
        pageData.put("size", result.getSize());
        pageData.put("totalElements", result.getTotalElements());
        pageData.put("totalPages", result.getTotalPages());

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("success", true);
        response.put(
                "message",
                "Lấy danh sách đợt giảm giá thành công"
        );
        response.put("data", pageData);

        return ResponseEntity.ok(response);
    }

    /**
     * Chi tiết đợt giảm giá theo mã.
     *
     * VD:
     * GET /api/dot-giam-gia/DGG001
     */
    @GetMapping("/{ma}")
    public ResponseEntity<Map<String, Object>> getByMa(
            @PathVariable String ma
    ) {

        DotGiamGiaResponse data =
                dotGiamGiaService.getByMa(ma);

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("success", true);
        response.put(
                "message",
                "Lấy chi tiết đợt giảm giá thành công"
        );
        response.put("data", data);

        return ResponseEntity.ok(response);
    }
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @Valid @RequestBody DotGiamGiaRequest request
    ) {

        DotGiamGiaResponse data =
                dotGiamGiaService.create(request);

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("success", true);
        response.put(
                "message",
                "Tạo đợt giảm giá thành công"
        );
        response.put("data", data);

        return ResponseEntity.ok(response);
    }
    @GetMapping("/san-pham-chi-tiet")
    public ResponseEntity<Map<String, Object>> getProductDetails(
            @RequestParam(required = false) String keyword
    ) {

        List<SanPhamChiTietGiamGiaResponse> data =
                dotGiamGiaService.getProductDetails(keyword);

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("success", true);

        response.put(
                "message",
                "Lấy danh sách biến thể sản phẩm thành công"
        );

        response.put("data", data);

        return ResponseEntity.ok(response);
    }
    @PutMapping("/{ma}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable String ma,
            @Valid @RequestBody DotGiamGiaRequest request
    ) {

        DotGiamGiaResponse data =
                dotGiamGiaService.update(ma, request);

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("success", true);
        response.put(
                "message",
                "Cập nhật đợt giảm giá thành công"
        );
        response.put("data", data);

        return ResponseEntity.ok(response);
    }
    @DeleteMapping("/{ma}")
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable String ma
    ) {

        DotGiamGiaResponse data =
                dotGiamGiaService.delete(ma);

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("success", true);
        response.put(
                "message",
                "Ngừng hoạt động đợt giảm giá thành công"
        );
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{ma}/trang-thai")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable String ma,
            @Valid @RequestBody DotGiamGiaStatusRequest request
    ) {

        DotGiamGiaResponse data =
                dotGiamGiaService.updateStatus(
                        ma,
                        request.getStatus()
                );

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("success", true);
        response.put(
                "message",
                "Cập nhật trạng thái đợt giảm giá thành công"
        );
        response.put("data", data);

        return ResponseEntity.ok(response);
    }
}
