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
public class VariantUpdateRequest {
    @Positive @JsonAlias({"idSanPham", "productId"}) private Long sanPhamId;
    @Size(max = 100) @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]*$")
    private String maChiTietSanPham;
    @NotBlank(message = "SKU không được trống") @Size(max = 100)
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]*$") private String sku;
    @NotNull @Positive @JsonAlias("idMauSac") private Long mauSacId;
    @NotNull @Positive @JsonAlias("idKichThuoc") private Long kichThuocId;
    @NotNull @Min(value = 0, message = "Số lượng không được nhỏ hơn 0") private Integer soLuong;
    @NotNull @DecimalMin(value = "0", inclusive = false, message = "Giá bán phải lớn hơn 0")
    @Digits(integer = 16, fraction = 2) private BigDecimal giaBan;
    @NotNull private Boolean kichHoat;
    @NotNull @Min(0) @Max(1) private Integer trangThai;

    public void setMaChiTietSanPham(String value) { maChiTietSanPham = value == null ? null : value.trim(); }
    public void setSku(String value) { sku = value == null ? null : value.trim(); }

    public VariantCreateRequest toVariantCreateRequest() {
        return new VariantCreateRequest(sanPhamId, maChiTietSanPham, sku, mauSacId, kichThuocId,
                soLuong, giaBan, kichHoat, trangThai);
    }
}
