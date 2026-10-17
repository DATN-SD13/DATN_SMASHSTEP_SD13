package com.smashstep.datn.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(onConstructor_ = @com.fasterxml.jackson.annotation.JsonCreator)
@AllArgsConstructor
public class SanPhamTrangThaiRequest {
    @NotNull(message = "Vui lòng chọn trạng thái.")
    @Min(value = 0, message = "Trạng thái phải là 0 hoặc 1.")
    @Max(value = 1, message = "Trạng thái phải là 0 hoặc 1.")
    private Integer trangThai;

    @com.fasterxml.jackson.annotation.JsonSetter("trangThai")
    public void setTrangThai(Object value) { trangThai = SoLuongSanPham.docTrangThai(value); }
}
