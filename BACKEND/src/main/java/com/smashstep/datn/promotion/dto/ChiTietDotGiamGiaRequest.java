package com.smashstep.datn.promotion.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ChiTietDotGiamGiaRequest {

    private Long idSanPhamChiTiet;

    private BigDecimal phanTramGiamBienThe;
}