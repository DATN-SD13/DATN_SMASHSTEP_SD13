package com.smashstep.datn.invoice.dto;

import com.smashstep.datn.invoice.entity.LichSuThanhToan;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Getter
@Builder
public class LichSuThanhToanDto {
    private Long id;
    private String phuongThucThanhToan;
    private BigDecimal soTien;
    private String maGiaoDich;
    private String thoiGian;
    private Integer maTrangThai;
    private String trangThai;
    private String moTa;

    public static LichSuThanhToanDto from(LichSuThanhToan item) {
        Integer status = item.getTrangThai();
        String statusLabel = status != null && status == 1 ? "Đã thanh toán" : "Chưa thanh toán";
        String method = item.getIdPhuongThucThanhToan() != null
                ? item.getIdPhuongThucThanhToan().getTenPhuongThuc()
                : "Chưa cập nhật";

        return LichSuThanhToanDto.builder()
                .id(item.getId())
                .phuongThucThanhToan(method)
                .soTien(item.getSoTien() == null ? BigDecimal.ZERO : item.getSoTien())
                .maGiaoDich(item.getMaGiaoDich())
                .thoiGian(item.getThoiGian() == null ? null : item.getThoiGian().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
                .maTrangThai(status)
                .trangThai(statusLabel)
                .moTa(item.getMoTa())
                .build();
    }
}
