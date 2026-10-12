package com.smashstep.datn.common.pricing;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public final class CurrentPrice {
    private final BigDecimal originalPrice;
    private final BigDecimal effectivePrice;
    private final BigDecimal discountPercent;
    private final boolean discounted;
    private final Long campaignId;
    private final String campaignCode;
    private final String campaignName;
}
