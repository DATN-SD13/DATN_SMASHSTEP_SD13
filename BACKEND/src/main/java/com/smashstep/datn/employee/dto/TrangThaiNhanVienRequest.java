package com.smashstep.datn.employee.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrangThaiNhanVienRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private Integer status;
}