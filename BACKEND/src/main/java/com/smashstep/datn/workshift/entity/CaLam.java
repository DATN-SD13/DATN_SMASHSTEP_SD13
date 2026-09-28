package com.smashstep.datn.workshift.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "ca_lam")
public class CaLam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_ca")
    private String maCa;

    @Column(name = "ten_ca")
    private String tenCa;

    @Column(name = "gio_bat_dau")
    private LocalTime gioBatDau;

    @Column(name = "gio_ket_thuc")
    private LocalTime gioKetThuc;

    @Column(name = "mo_ta")
    private String moTa;

    @Column(name = "trang_thai")
    private Integer trangThai;

}