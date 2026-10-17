package com.smashstep.datn.product.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor(onConstructor_ = @JsonCreator)
@AllArgsConstructor
public class SanPhamChiTietSuaRequest {
    @Positive(message = "Vui lòng chọn sản phẩm hợp lệ.") @JsonAlias({"idSanPham", "productId"}) private Long sanPhamId;
    @Size(max = 100, message = "Mã biến thể tối đa 100 ký tự.") @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]*$", message = "Mã chỉ gồm chữ Latin, số, dấu chấm, gạch dưới hoặc gạch ngang.")
    private String maChiTietSanPham;
    @NotBlank(message = "SKU không được trống") @Size(max = 100, message = "SKU tối đa 100 ký tự.")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]*$", message = "Mã chỉ gồm chữ Latin, số, dấu chấm, gạch dưới hoặc gạch ngang.") private String sku;
    @NotNull(message = "Vui lòng chọn màu sắc.") @Positive(message = "Vui lòng chọn màu sắc hợp lệ.") @JsonAlias("idMauSac") private Long mauSacId;
    @NotNull(message = "Vui lòng chọn kích thước.") @Positive(message = "Vui lòng chọn kích thước hợp lệ.") @JsonAlias("idKichThuoc") private Long kichThuocId;
    @NotNull(message = "Số lượng không được để trống.") @Min(value = 0, message = "Số lượng không được nhỏ hơn 0") private Integer soLuong;
    @NotNull(message = "Giá bán không được để trống.") @DecimalMin(value = "0", inclusive = false, message = "Giá bán phải lớn hơn 0")
    @Digits(integer = 16, fraction = 2, message = "Giá bán tối đa 16 chữ số phần nguyên và 2 chữ số thập phân.") private BigDecimal giaBan;
    @NotNull(message = "Kích hoạt không được để trống.") private Boolean kichHoat;
    @NotNull(message = "Trạng thái không được để trống.") @Min(value = 0, message = "Trạng thái phải là 0 hoặc 1.") @Max(value = 1, message = "Trạng thái phải là 0 hoặc 1.") private Integer trangThai;

    @com.fasterxml.jackson.annotation.JsonSetter("sanPhamId")
    public void setSanPhamId(Object value) { sanPhamId = SoLuongSanPham.docId(value); }

    @com.fasterxml.jackson.annotation.JsonSetter("mauSacId")
    public void setMauSacId(Object value) { mauSacId = SoLuongSanPham.docId(value); }

    @com.fasterxml.jackson.annotation.JsonSetter("kichThuocId")
    public void setKichThuocId(Object value) { kichThuocId = SoLuongSanPham.docId(value); }

    @com.fasterxml.jackson.annotation.JsonSetter("trangThai")
    public void setTrangThai(Object value) { trangThai = SoLuongSanPham.docTrangThai(value); }

    public void setMaChiTietSanPham(String value) { maChiTietSanPham = value == null ? null : value.trim(); }
    @com.fasterxml.jackson.annotation.JsonSetter("soLuong")
    public void setSoLuong(Object value) { soLuong = SoLuongSanPham.doc(value); }

    public void setSku(String value) { sku = value == null ? null : value.trim(); }

    public SanPhamChiTietThemRequest chuyenSangThemRequest() {
        return new SanPhamChiTietThemRequest(sanPhamId, maChiTietSanPham, sku, mauSacId, kichThuocId,
                soLuong, giaBan, kichHoat, trangThai);
    }
}
