package com.smashstep.datn.promotion.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChiTietDotGiamGiaResponse {

    private Long id;

    private Long idSanPhamChiTiet;

    private String maChiTietSanPham;

    private String sku;

    private BigDecimal giaBan;

    private BigDecimal phanTramGiamBienThe;

    private Integer trangThai;
}