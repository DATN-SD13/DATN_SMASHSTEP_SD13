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
    private Long employeeId; // Legacy input accepted but always replaced by authenticated actor at the API boundary.
    @Positive private Long paymentMethodId;
    @Positive private Long invoiceId;
    @Positive private Long revision;
    @Valid private SalesDelivery delivery;
    @Size(max = 2) @Valid private List<@NotNull Payment> payments;
    private Boolean transferConfirmed;
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
        @com.fasterxml.jackson.annotation.JsonSetter("quantity")
        public void readQuantity(BigDecimal value) {
            try { quantity = value == null ? null : value.intValueExact(); }
            catch (ArithmeticException fractional) { throw new IllegalArgumentException("Số lượng phải là số nguyên", fractional); }
        }
    }
    @Getter @Setter
    public static class Payment {
        @NotNull @Positive private Long methodId;
        @NotNull @DecimalMin("0") @Digits(integer = 16, fraction = 2) private BigDecimal amount;
    }
}
