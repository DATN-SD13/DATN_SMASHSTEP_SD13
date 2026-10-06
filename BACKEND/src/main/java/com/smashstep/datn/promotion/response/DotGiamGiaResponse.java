package com.smashstep.datn.promotion.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class DotGiamGiaResponse {

    private Long id;
    private String code;
    private String name;
    private BigDecimal discountValue;
    private LocalDate startDate;
    private LocalDate endDate;

    private Integer status;
    private String statusLabel;

    // THÊM DÒNG NÀY
    private String timeStatus;

    private String description;

    private List<Long> productDetailIds;
}