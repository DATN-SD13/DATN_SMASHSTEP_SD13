package com.smashstep.datn.promotion.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class PhieuGiamGiaResponse {

    private Long id;

    private String code;

    private String name;

    private Integer form;
    private String formLabel;

    private Integer discountType;
    private String discountTypeLabel;

    private BigDecimal discountValue;

    private BigDecimal minOrderValue;

    private BigDecimal maxDiscount;

    private String startDate;
    private String endDate;

    private Integer quantity;
    private Integer usedQuantity;

    private Integer status;
    private String statusLabel;

    private String description;
// vo han phieu
    private Boolean unlimited;
    private List<Long> customerIds;
}
