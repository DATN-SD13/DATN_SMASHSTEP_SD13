package com.smashstep.datn.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "san_pham")
public class SanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_danh_muc")
    private DanhMuc idDanhMuc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_thuong_hieu")
    private ThuongHieu idThuongHieu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_chat_lieu")
    private ChatLieu idChatLieu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_kieu_dang")
    private KieuDang idKieuDang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_co_giay")
    private CoGiay idCoGiay;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_xuat_xu")
    private XuatXu idXuatXu;

    @Column(name = "ma_san_pham")
    private String maSanPham;

    @Column(name = "ten_san_pham")
    private String tenSanPham;

    @Column(name = "mo_ta_chi_tiet")
    private String moTaChiTiet;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;

    @Column(name = "xoa_mem")
    private Boolean xoaMem;

    @Column(name = "trang_thai")
    private Integer trangThai;

    @Column(name = "nguoi_tao")
    private Long nguoiTao;

    @Column(name = "nguoi_cap_nhat")
    private Long nguoiCapNhat;

}