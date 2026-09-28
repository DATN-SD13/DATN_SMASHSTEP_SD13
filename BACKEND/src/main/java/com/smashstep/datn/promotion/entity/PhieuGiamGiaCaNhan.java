package com.smashstep.datn.promotion.entity;

import com.smashstep.datn.customer.entity.KhachHang;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "phieu_giam_gia_ca_nhan")
public class PhieuGiamGiaCaNhan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_khach_hang")
    private KhachHang idKhachHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_phieu_giam_gia")
    private PhieuGiamGia idPhieuGiamGia;

    @Column(name = "trang_thai")
    private Integer trangThai;

    @Column(name = "da_su_dung")
    private Boolean daSuDung;

    @Column(name = "ma_phieu_giam_gia_ca_nhan")
    private String maPhieuGiamGiaCaNhan;

    @Column(name = "ngay_nhan")
    private LocalDateTime ngayNhan;

    @Column(name = "ngay_su_dung")
    private LocalDateTime ngaySuDung;

}