package com.smashstep.datn.payment.entity;

import com.smashstep.datn.invoice.entity.HoaDon;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "thanh_toan_hoa_don")
public class ThanhToanHoaDon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_hoa_don")
    private HoaDon idHoaDon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_hinh_thuc_thanh_toan")
    private HinhThucThanhToan idHinhThucThanhToan;

    @Column(name = "ma_giao_dich_thanh_toan")
    private String maGiaoDichThanhToan;

    @Column(name = "so_tien")
    private BigDecimal soTien;

    @Column(name = "trang_thai")
    private Integer trangThai;

    @Column(name = "ma_yeu_cau")
    private String maYeuCau;

    @Column(name = "ma_giao_dich_ngoai")
    private String maGiaoDichNgoai;

    @Column(name = "ma_tham_chieu")
    private String maThamChieu;

    @Column(name = "duong_dan_thanh_toan")
    private String duongDanThanhToan;

    @Column(name = "ma_qr")
    private String maQr;

    @Column(name = "thoi_gian_het_han")
    private LocalDateTime thoiGianHetHan;

    @Column(name = "du_lieu_phan_hoi")
    private String duLieuPhanHoi;

    @Column(name = "thoi_gian_tao")
    private LocalDateTime thoiGianTao;

    @Column(name = "thoi_gian_cap_nhat")
    private LocalDateTime thoiGianCapNhat;

    @Column(name = "ghi_chu")
    private String ghiChu;

}