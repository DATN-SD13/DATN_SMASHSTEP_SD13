package com.smashstep.datn.product.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(onConstructor_ = @JsonCreator)
@AllArgsConstructor
public class SanPhamThemRequest {
    @NotBlank(message = "Mã sản phẩm không được trống")
    @Size(max = 50, message = "Mã sản phẩm tối đa 50 ký tự.")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]*$", message = "Mã chỉ gồm chữ Latin, số, dấu chấm, gạch dưới hoặc gạch ngang")
    private String maSanPham;
    @NotBlank(message = "Tên sản phẩm không được trống")
    @Size(max = 255, message = "Tên sản phẩm tối đa 255 ký tự.")
    private String tenSanPham;
    @NotNull(message = "Vui lòng chọn danh mục.") @Positive(message = "Vui lòng chọn danh mục hợp lệ.")
    private Long danhMucId;
    @NotNull(message = "Vui lòng chọn thương hiệu.") @Positive(message = "Vui lòng chọn thương hiệu hợp lệ.")
    private Long thuongHieuId;
    @NotNull(message = "Vui lòng chọn chất liệu.") @Positive(message = "Vui lòng chọn chất liệu hợp lệ.")
    private Long chatLieuId;
    @NotNull(message = "Vui lòng chọn kiểu dáng.") @Positive(message = "Vui lòng chọn kiểu dáng hợp lệ.")
    private Long kieuDangId;
    @NotNull(message = "Vui lòng chọn cổ giày.") @Positive(message = "Vui lòng chọn cổ giày hợp lệ.")
    private Long coGiayId;
    @NotNull(message = "Vui lòng chọn xuất xứ.") @Positive(message = "Vui lòng chọn xuất xứ hợp lệ.")
    private Long xuatXuId;
    private String moTaChiTiet;
    @NotNull(message = "Trạng thái không được để trống.") @Min(value = 0, message = "Trạng thái phải là 0 hoặc 1.") @Max(value = 1, message = "Trạng thái phải là 0 hoặc 1.")
    private Integer trangThai;

    @com.fasterxml.jackson.annotation.JsonSetter("danhMucId")
    public void setDanhMucId(Object value) { danhMucId = SoLuongSanPham.docId(value); }

    @com.fasterxml.jackson.annotation.JsonSetter("thuongHieuId")
    public void setThuongHieuId(Object value) { thuongHieuId = SoLuongSanPham.docId(value); }

    @com.fasterxml.jackson.annotation.JsonSetter("chatLieuId")
    public void setChatLieuId(Object value) { chatLieuId = SoLuongSanPham.docId(value); }

    @com.fasterxml.jackson.annotation.JsonSetter("kieuDangId")
    public void setKieuDangId(Object value) { kieuDangId = SoLuongSanPham.docId(value); }

    @com.fasterxml.jackson.annotation.JsonSetter("coGiayId")
    public void setCoGiayId(Object value) { coGiayId = SoLuongSanPham.docId(value); }

    @com.fasterxml.jackson.annotation.JsonSetter("xuatXuId")
    public void setXuatXuId(Object value) { xuatXuId = SoLuongSanPham.docId(value); }

    @com.fasterxml.jackson.annotation.JsonSetter("trangThai")
    public void setTrangThai(Object value) { trangThai = SoLuongSanPham.docTrangThai(value); }

    public void setMaSanPham(String value) { maSanPham = value == null ? null : value.trim(); }
}
