package com.smashstep.datn.promotion.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PhieuGiamGiaRequest {

    private String code;

    private String name;

    // 1 = Công khai
    // 2 = Cá nhân
    private Integer form;

    // 1 = Phần trăm
    // 2 = Tiền mặt
    private Integer discountType;

    private BigDecimal discountValue;

    private BigDecimal minOrderValue;

    private BigDecimal maxDiscount;

    private String startDate;

    private String endDate;

    private Integer quantity;

    private Integer status;

    private String description;
}