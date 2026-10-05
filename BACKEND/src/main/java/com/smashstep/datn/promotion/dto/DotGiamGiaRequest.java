package com.smashstep.datn.promotion.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class DotGiamGiaRequest {

    private String maDotGiamGia;

    private String tenDotGiamGia;

    private BigDecimal phanTramGiamDot;

    private LocalDateTime ngayBatDau;

    private LocalDateTime ngayKetThuc;

    private Boolean kichHoat;

    private String moTa;

    private List<ChiTietDotGiamGiaRequest> chiTiet;
}