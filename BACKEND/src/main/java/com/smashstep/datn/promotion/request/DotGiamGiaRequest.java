
package com.smashstep.datn.promotion.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class DotGiamGiaRequest {

    // Khi tạo mới backend tự sinh mã
    private String code;

    @NotBlank(message = "Tên đợt giảm giá không được để trống")
    @Size(max = 255, message = "Tên đợt giảm giá tối đa 255 ký tự")
    private String name;

    @NotNull(message = "Giá trị giảm không được để trống")
    @DecimalMin(
            value = "0.01",
            message = "Giá trị giảm phải lớn hơn 0"
    )
    @DecimalMax(
            value = "100.00",
            message = "Giá trị giảm không được vượt quá 100%"
    )
    private BigDecimal discountValue;

    // Ngày + giờ + phút + giây
    @NotNull(message = "Thời gian bắt đầu không được để trống")
    private LocalDateTime startDate;

    @NotNull(message = "Thời gian kết thúc không được để trống")
    private LocalDateTime endDate;

    @Size(max = 1000)
    private String description;

    @NotEmpty(message = "Vui lòng chọn ít nhất một biến thể sản phẩm")
    @Size(max = 1000)
    private List<@NotNull @Positive Long> productDetailIds;
    // Preserve the previous date-only API while accepting the team's datetime inputs.
    @com.fasterxml.jackson.annotation.JsonSetter("startDate")
    public void readStartDate(Object value) { startDate = dateTime(value, false); }
    @com.fasterxml.jackson.annotation.JsonSetter("endDate")
    public void readEndDate(Object value) { endDate = dateTime(value, true); }
    private static LocalDateTime dateTime(Object value, boolean end) {
        if (value == null) return null;
        if (value instanceof LocalDateTime time) return time;
        if (!(value instanceof String text)) throw new IllegalArgumentException("Thời gian không hợp lệ");
        if (text.length() == 10) {
            var date = java.time.LocalDate.parse(text);
            return end ? date.atTime(23, 59, 59) : date.atStartOfDay();
        }
        return LocalDateTime.parse(text);
    }

}
