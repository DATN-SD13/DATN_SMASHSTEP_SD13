package com.smashstep.datn.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record PendingSaleRequest(
        @NotNull @Positive Long revision,
        @Positive Long customerId,
        @Size(max = 980) String note,
        @NotNull @Size(max = 50) List<@NotNull @Valid Item> items,
        @Valid SalesDelivery delivery,
        @Size(max = 50) String voucherCode) {
    public PendingSaleRequest(Long revision, Long customerId, String note, List<Item> items) {
        this(revision, customerId, note, items, null, null);
    }
    // BigDecimal plus explicit exact conversion rejects fractional JSON quantities rather than truncating them.
    public record Item(@NotNull @Positive Long variantId,
                       @NotNull @DecimalMin("1") @DecimalMax("10000") @Digits(integer = 5, fraction = 0) BigDecimal quantity,
                       @NotNull @DecimalMin("0") @Digits(integer = 16, fraction = 2) BigDecimal unitPrice) {}
}
