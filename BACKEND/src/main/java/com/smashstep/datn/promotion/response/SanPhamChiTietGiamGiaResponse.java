package com.smashstep.datn.promotion.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SanPhamChiTietGiamGiaResponse {

    private Long id;

    private String productCode;

    private String productName;

    private String productDetailCode;

    private String sku;

    private String color;

    private String colorHex;

    private String size;

    private BigDecimal price;

    private Integer quantity;

    private Integer status;
}