package com.smashstep.datn.product.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(onConstructor_ = @JsonCreator)
@AllArgsConstructor
public class SanPhamSuaRequest {
    @Size(max = 50)
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]*$")
    private String maSanPham;
    @NotBlank(message = "Tên sản phẩm không được trống") @Size(max = 255)
    private String tenSanPham;
    @NotNull @Positive private Long danhMucId;
    @NotNull @Positive private Long thuongHieuId;
    @NotNull @Positive private Long chatLieuId;
    @NotNull @Positive private Long kieuDangId;
    @NotNull @Positive private Long coGiayId;
    @NotNull @Positive private Long xuatXuId;
    private String moTaChiTiet;
    @NotNull @Min(0) @Max(1) private Integer trangThai;

    public void setMaSanPham(String value) { maSanPham = value == null ? null : value.trim(); }

    public SanPhamThemRequest chuyenSangThemRequest() {
        return new SanPhamThemRequest(maSanPham, tenSanPham, danhMucId, thuongHieuId, chatLieuId,
                kieuDangId, coGiayId, xuatXuId, moTaChiTiet, trangThai);
    }
}
