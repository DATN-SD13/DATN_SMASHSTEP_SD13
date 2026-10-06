package com.smashstep.datn.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class SalesRequest {
    @NotBlank
    @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    private String requestId;
    @Positive private Long customerId;
    @NotNull @Positive private Long employeeId;
    @NotNull @Positive private Long paymentMethodId;
    @Size(max = 50) private String voucherCode;
    @DecimalMin("0") @Digits(integer = 16, fraction = 2) private BigDecimal paidAmount;
    @Size(max = 1000) private String note;
    @NotEmpty @Size(max = 50) @Valid private List<@NotNull Item> items;

    @Getter
    @Setter
    public static class Item {
        @NotNull @Positive private Long variantId;
        @NotNull @Min(1) @Max(10000) private Integer quantity;
        @NotNull @DecimalMin("0") @Digits(integer = 16, fraction = 2)
        private BigDecimal unitPrice;
    }
}
