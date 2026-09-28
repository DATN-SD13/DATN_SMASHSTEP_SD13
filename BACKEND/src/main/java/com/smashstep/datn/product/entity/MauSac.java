package com.smashstep.datn.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "mau_sac")
public class MauSac {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_mau")
    private String maMau;

    @Column(name = "ten_mau")
    private String tenMau;

    @Column(name = "trang_thai")
    private Integer trangThai;

    @Column(name = "xoa_mem")
    private Boolean xoaMem;

}