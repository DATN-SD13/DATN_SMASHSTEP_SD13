package com.smashstep.datn.promotion.dto;

import lombok.Getter;
import jakarta.validation.constraints.*;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ChiTietDotGiamGiaRequest {

    @NotNull @Positive
    private Long idSanPhamChiTiet;

    @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 2)
    private BigDecimal phanTramGiamBienThe;
}