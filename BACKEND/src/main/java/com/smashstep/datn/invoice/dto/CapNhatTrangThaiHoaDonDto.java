package com.smashstep.datn.invoice.dto;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Getter
@Setter
public class CapNhatTrangThaiHoaDonDto {
    @Positive
    private Long idNhanVien;
    @NotNull(message = "Trạng thái không được để trống")
    private Integer trangThai;
    @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
    private String ghiChu;
}
