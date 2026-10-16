package com.smashstep.datn.schedule.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
public class CaLamViec {

    private Long id;

    private String maCa;

    private String tenCa;

    private LocalTime gioBatDau;

    private LocalTime gioKetThuc;

    private String moTa;

    private Integer trangThai;

    private LocalDateTime ngayTao;

    private LocalDateTime ngayCapNhat;
}
