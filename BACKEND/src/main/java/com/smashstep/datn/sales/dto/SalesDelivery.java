package com.smashstep.datn.sales.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record SalesDelivery(boolean enabled, @Positive Long addressId,
        @Size(max = 255) String recipientName, @Size(max = 20) String recipientPhone,
        @Size(max = 500) String address,
        @DecimalMin("0") @Digits(integer = 16, fraction = 2) BigDecimal shippingFee) {}
