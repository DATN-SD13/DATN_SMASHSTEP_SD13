package com.smashstep.datn.employee.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "chuc_nang")
public class ChucNang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_chuc_nang")
    private String maChucNang;

    @Column(name = "ten_chuc_nang")
    private String tenChucNang;

    @Column(name = "mo_ta")
    private String moTa;

    @Column(name = "trang_thai")
    private Integer trangThai;

}