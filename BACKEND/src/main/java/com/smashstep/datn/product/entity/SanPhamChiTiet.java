package com.smashstep.datn.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "san_pham_chi_tiet")
public class SanPhamChiTiet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_san_pham")
    private SanPham idSanPham;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_mau_sac")
    private MauSac idMauSac;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_kich_thuoc")
    private KichThuoc idKichThuoc;

    @Column(name = "ma_chi_tiet_sp")
    private String maChiTietSp;

    @Column(name = "so_luong")
    private Integer soLuong;

    @Column(name = "gia_nem_yet")
    private BigDecimal giaNemYet;

    @Column(name = "gia_ban")
    private BigDecimal giaBan;

    @Column(name = "ghi_chu")
    private String ghiChu;

    @Column(name = "trang_thai")
    private Integer trangThai;

    @Column(name = "xoa_mem")
    private Boolean xoaMem;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;

    @Column(name = "nguoi_tao")
    private Long nguoiTao;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    @Column(name = "nguoi_cap_nhat")
    private Long nguoiCapNhat;

}