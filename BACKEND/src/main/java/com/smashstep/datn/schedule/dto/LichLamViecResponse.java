package com.smashstep.datn.schedule.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class LichLamViecResponse {
    private Long       id;
    private String     maNhanVien;
    private String     tenNhanVien;
    private String     hinhAnh;
    private String     vaiTro;         // tenVaiTro của nhân viên
    private Long       caLamViecId;
    private String     maCa;
    private String     tenCa;
    private String     thoiGian;       // "08:00 - 12:00"
    private String     ngayLam;        // "yyyy-MM-dd"
    private String     thoiGianBatDau; // "HH:mm dd/MM/yyyy"
    private String     thoiGianKetThuc;// "HH:mm dd/MM/yyyy"
    private BigDecimal doanhThu;       // tổng thanhTien hóa đơn trong ca
    private Integer    trangThai;
    private String     trangThaiLabel;
    private String     ghiChu;
}
