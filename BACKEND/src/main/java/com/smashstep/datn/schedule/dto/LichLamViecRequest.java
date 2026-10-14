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
    @NotNull(message = "Danh sách nhân viên không được để trống")
    private List<String> maNhanViens;

    @NotNull(message = "Ca làm việc không được để trống")
    private Long caLamViecId;

    @NotNull(message = "Ngày làm không được để trống")
    private LocalDate ngayLam;

    private String ghiChu;
}
