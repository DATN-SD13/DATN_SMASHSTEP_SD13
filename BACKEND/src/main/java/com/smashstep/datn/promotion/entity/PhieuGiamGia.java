package com.smashstep.datn.promotion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "phieu_giam_gia")
public class PhieuGiamGia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_voucher")
    private String maVoucher;

    @Column(name = "ten_voucher")
    private String tenVoucher;

    @Column(name = "loai_giam_gia")
    private Integer loaiGiamGia;

    @Column(name = "gia_tri_giam")
    private BigDecimal giaTriGiam;

    @Column(name = "giam_toi_da")
    private BigDecimal giamToiDa;

    @Column(name = "don_toi_thieu")
    private BigDecimal donToiThieu;

    @Column(name = "so_luong")
    private Integer soLuong;

    @Column(name = "so_luong_da_dung")
    private Integer soLuongDaDung;

    @Column(name = "loai_phieu")
    private Integer loaiPhieu;

    @Column(name = "ngay_bat_dau")
    private LocalDateTime ngayBatDau;

    @Column(name = "ngay_ket_thuc")
    private LocalDateTime ngayKetThuc;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;

    @Column(name = "trang_thai")
    private Integer trangThai;

}