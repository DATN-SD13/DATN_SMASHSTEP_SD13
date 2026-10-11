package com.smashstep.datn.promotion.dto;

import lombok.Getter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class DotGiamGiaRequest {

    @Size(max = 50)
    private String maDotGiamGia;

    @NotBlank @Size(max = 255)
    private String tenDotGiamGia;

    @NotNull @DecimalMin("0.01") @DecimalMax("100") @Digits(integer = 3, fraction = 2)
    private BigDecimal phanTramGiamDot;

    @NotNull
    private LocalDateTime ngayBatDau;

    @NotNull
    private LocalDateTime ngayKetThuc;

    private Boolean kichHoat;

    @Size(max = 1000)
    private String moTa;

    @NotEmpty @Size(max = 1000)
    private List<@NotNull @Valid ChiTietDotGiamGiaRequest> chiTiet;
}