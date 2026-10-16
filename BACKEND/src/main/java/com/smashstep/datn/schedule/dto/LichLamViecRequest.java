package com.smashstep.datn.schedule.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class LichLamViecRequest {

    /** Danh sách mã nhân viên */
    @jakarta.validation.constraints.NotEmpty(message = "Danh sách nhân viên không được để trống")
    @jakarta.validation.constraints.Size(max = 100)
    private List<@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=50) String> maNhanViens;

    @NotNull(message = "Ca làm việc không được để trống")
    @jakarta.validation.constraints.Positive
    private Long caLamViecId;

    @NotNull(message = "Ngày làm không được để trống")
    private LocalDate ngayLam;

    @jakarta.validation.constraints.Size(max=500)
    private String ghiChu;
}
