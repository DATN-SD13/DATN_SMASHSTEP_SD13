package com.smashstep.datn.schedule.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KetThucCaRequest {
    @jakarta.validation.constraints.Size(max=500)
    private String ghiChuKetCa;  // Không bắt buộc - chỉ có field này thôi
    @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.DecimalMin("0")
    @jakarta.validation.constraints.Digits(integer=15,fraction=2)
    private java.math.BigDecimal tienMatThucTe;
}
