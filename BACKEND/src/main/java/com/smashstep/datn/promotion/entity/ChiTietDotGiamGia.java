package com.smashstep.datn.promotion.entity;

import com.smashstep.datn.product.entity.SanPhamChiTiet;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "chi_tiet_dot_giam_gia")
public class ChiTietDotGiamGia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_dot_giam_gia")
    private DotGiamGia idDotGiamGia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_chi_tiet_san_pham")
    private SanPhamChiTiet idChiTietSanPham;

    @Column(name = "so_luong_ap_dung")
    private Integer soLuongApDung;

    @Column(name = "gia_tri_giam")
    private BigDecimal giaTriGiam;

    @Column(name = "so_tien_da_giam")
    private BigDecimal soTienDaGiam;

    @Column(name = "trang_thai")
    private Integer trangThai;

    @Column(name = "ghi_chu")
    private String ghiChu;

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