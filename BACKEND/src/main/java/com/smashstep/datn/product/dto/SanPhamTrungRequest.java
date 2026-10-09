package com.smashstep.datn.product.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SanPhamTrungRequest {
    @NotBlank @Size(max = 255)
    private String tenSanPham;
    @NotNull @Positive private Long danhMucId;
    @NotNull @Positive private Long thuongHieuId;
    @NotNull @Positive private Long chatLieuId;
    @NotNull @Positive private Long kieuDangId;
    @NotNull @Positive private Long coGiayId;
    @NotNull @Positive private Long xuatXuId;
    @Positive private Long excludeId;

    public static SanPhamTrungRequest from(SanPhamThemRequest request, Long excludeId) {
        return new SanPhamTrungRequest(request.getTenSanPham(), request.getDanhMucId(),
                request.getThuongHieuId(), request.getChatLieuId(), request.getKieuDangId(),
                request.getCoGiayId(), request.getXuatXuId(), excludeId);
    }
}
