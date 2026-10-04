package com.smashstep.datn.product.repository;

import java.math.BigDecimal;

public interface TongHopSanPham {
    Long getProductId();
    Long getQuantity();
    BigDecimal getMinPrice();
    BigDecimal getMaxPrice();
    Long getVariantCount();
    Long getColorCount();
    Long getSizeCount();
}
