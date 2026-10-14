package com.smashstep.datn.schedule.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LichLamViecResponse {
    private Long   id;
    private String maNhanVien;
    private String tenNhanVien;
    private String hinhAnh;
    private Long   caLamViecId;
    private String maCa;
    private String tenCa;
    private String thoiGian;     // "08:00 - 12:00"
    private String ngayLam;      // "yyyy-MM-dd"
    private Integer trangThai;
    private String trangThaiLabel;
    private String ghiChu;
}
