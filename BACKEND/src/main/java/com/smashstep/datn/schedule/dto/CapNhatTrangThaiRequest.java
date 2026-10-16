package com.smashstep.datn.schedule.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CapNhatTrangThaiRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private Integer trangThai;

    @jakarta.validation.constraints.Size(max=500)
    private String ghiChu;
}
