package com.smashstep.datn.promotion.entity;

import com.smashstep.datn.customer.entity.KhachHang;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "phieu_giam_gia_chi_tiet")
public class PhieuGiamGiaChiTiet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_phieu_giam_gia")
    private PhieuGiamGia idPhieuGiamGia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_khach_hang")
    private KhachHang idKhachHang;

    @Column(name = "trang_thai")
    private Integer trangThai;

}