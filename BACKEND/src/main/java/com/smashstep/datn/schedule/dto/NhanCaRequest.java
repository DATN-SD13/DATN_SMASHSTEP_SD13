package com.smashstep.datn.schedule.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class NhanCaRequest {
    @NotNull @DecimalMin("0") @Digits(integer=15,fraction=2)
    private BigDecimal tienMatDauCa;
}
