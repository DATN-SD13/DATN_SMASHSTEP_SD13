package com.smashstep.datn.product.repository;

import java.math.BigDecimal;

public interface InventorySummary {
    Long getProductId();
    Long getQuantity();
    BigDecimal getMinPrice();
    BigDecimal getMaxPrice();
    Long getVariantCount();
}

