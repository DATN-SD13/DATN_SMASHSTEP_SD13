package com.smashstep.datn.schedule.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CaLamViecResponse {
    private Long   id;
    private String maCa;
    private String tenCa;
    private String gioBatDau;    // "HH:mm"
    private String gioKetThuc;   // "HH:mm"
    private String thoiGian;     // "HH:mm - HH:mm"
    private String moTa;
    private Integer trangThai;
    private String trangThaiLabel;
}
