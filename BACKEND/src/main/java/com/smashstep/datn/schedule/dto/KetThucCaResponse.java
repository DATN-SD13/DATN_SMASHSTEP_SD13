package com.smashstep.datn.schedule.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class KetThucCaResponse {
    private Long lichLamViecId;
    private String tenCa;
    private String gioBatDau;
    private String gioKetThuc;
    private LocalDate ngayLam;
    private LocalDateTime thoiGianBatDau;
    private LocalDateTime thoiGianKetThuc;
    private BigDecimal tienMatCuoiCa;
    private BigDecimal chuyenKhoanCuoiCa;
    private BigDecimal tongTienCuoiCa;
    private BigDecimal doanhThuCa;  // Tổng doanh thu trong ca
    private String ghiChuKetCa;
}
