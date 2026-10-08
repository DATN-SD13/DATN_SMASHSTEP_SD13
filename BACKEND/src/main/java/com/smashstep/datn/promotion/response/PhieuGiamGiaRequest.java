package com.smashstep.datn.promotion.response;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class PhieuGiamGiaRequest {

    @Size(max = 50) private String code;

    @NotBlank @Size(max = 255) private String name;

    // 1 = Công khai
    // 2 = Cá nhân
    @NotNull @Min(1) @Max(2) private Integer form;

    // 1 = Phần trăm
    // 2 = Tiền mặt
    @NotNull @Min(1) @Max(2) private Integer discountType;

    @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 16, fraction = 2)
    private BigDecimal discountValue;

    @DecimalMin("0") @Digits(integer = 16, fraction = 2) private BigDecimal minOrderValue;

    @DecimalMin("0") @Digits(integer = 16, fraction = 2) private BigDecimal maxDiscount;

    @Pattern(
            regexp = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}",
            message = "Ngày giờ bắt đầu không đúng định dạng"
    )
    private String startDate;

    @Pattern(
            regexp = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}",
            message = "Ngày giờ kết thúc không đúng định dạng"
    )
    private String endDate;
    

    private Integer quantity;

    private Integer status;

    @Size(max = 1000) private String description;

    private Boolean unlimited;
    @Size(max = 1000) private List<@NotNull @Positive Long> customerIds;
}
