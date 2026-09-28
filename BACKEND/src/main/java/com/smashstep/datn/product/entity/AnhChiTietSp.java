package com.smashstep.datn.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "anh_chi_tiet_sp")
public class AnhChiTietSp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_chi_tiet_san_pham")
    private SanPhamChiTiet idChiTietSanPham;

    @Column(name = "duong_san_sp")
    private String duongSanSp;

    @Column(name = "anh_dai_dien")
    private String anhDaiDien;

    @Column(name = "mo_ta")
    private String moTa;

    @Column(name = "xoa_mem")
    private Boolean xoaMem;

}