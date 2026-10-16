package com.smashstep.datn.schedule.entity;

import com.smashstep.datn.employee.entity.NhanVien;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class LichLamViec {

    private Long id;

    private NhanVien idNhanVien;

    private CaLamViec idCaLamViec;

    private LocalDate ngayLam;

    /** 0=Chờ, 1=Xác nhận, 2=Vắng, 3=Hoàn thành */
    private Integer trangThai;

    private String ghiChu;

    private LocalDateTime ngayTao;

    private LocalDateTime ngayCapNhat;
}
