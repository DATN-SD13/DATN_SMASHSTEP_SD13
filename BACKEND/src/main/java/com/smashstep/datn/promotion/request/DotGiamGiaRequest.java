package com.smashstep.datn.promotion.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class DotGiamGiaRequest {

    @NotBlank(message = "Mã đợt giảm giá không được để trống")
    private String code;

    @NotBlank(message = "Tên đợt giảm giá không được để trống")
    private String name;

    @NotNull(message = "Giá trị giảm không được để trống")
    @DecimalMin(value = "0.01", message = "Giá trị giảm phải lớn hơn 0")
    @DecimalMax(value = "100.00", message = "Giá trị giảm không được vượt quá 100%")
    private BigDecimal discountValue;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDate endDate;

    private String description;

    private List<Long> productDetailIds;
}