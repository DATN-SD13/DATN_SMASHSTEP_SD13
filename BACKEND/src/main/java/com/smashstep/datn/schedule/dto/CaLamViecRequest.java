package com.smashstep.datn.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CaLamViecRequest {

    @NotBlank(message = "Ten ca khong duoc de trong")
    private String tenCa;

    @NotNull(message = "Gio bat dau khong duoc de trong")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Gio bat dau khong hop le (HH:mm)")
    private String gioBatDau;

    @NotNull(message = "Gio ket thuc khong duoc de trong")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Gio ket thuc khong hop le (HH:mm)")
    private String gioKetThuc;

    private String moTa;
}