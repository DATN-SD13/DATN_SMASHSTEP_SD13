package com.smashstep.datn.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "kieu_dang")
public class KieuDang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_kieu_dang")
    private String maKieuDang;

    @Column(name = "ten_kieu_dang")
    private String tenKieuDang;

    @Column(name = "trang_thai")
    private Integer trangThai;

}
