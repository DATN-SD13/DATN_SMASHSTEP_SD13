
package com.smashstep.datn.promotion.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class DotGiamGiaResponse {

    private Long id;
    private String code;
    private String name;
    private BigDecimal discountValue;

    // Ngày + giờ + phút + giây
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private Integer status;
    private String statusLabel;
    private String timeStatus;

    private String description;

    private List<Long> productDetailIds;
}
