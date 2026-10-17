package com.smashstep.datn.invoice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CapNhatTrangThaiHoaDonDto {
    @Positive private Long idNhanVien;

    @NotNull(message = "Trạng thái không được để trống")
    private Integer trangThai;

    @jakarta.validation.constraints.Pattern(
            regexp = "POS|HOA_DON",
            message = "Nguồn thao tác không hợp lệ")
    private String nguonThaoTac;

    @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
    private String ghiChu;
}
