package com.smashstep.datn.promotion.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DotGiamGiaDetailResponse {

    private Long id;

    private String maDotGiamGia;

    private String tenDotGiamGia;

    private BigDecimal phanTramGiamDot;

    private LocalDateTime ngayBatDau;

    private LocalDateTime ngayKetThuc;

    private Boolean kichHoat;

    private Integer trangThai;

    private String trangThaiText;

    private String moTa;

    private LocalDateTime ngayTao;

    private LocalDateTime ngayCapNhat;

    private List<ChiTietDotGiamGiaResponse> chiTiet;
}