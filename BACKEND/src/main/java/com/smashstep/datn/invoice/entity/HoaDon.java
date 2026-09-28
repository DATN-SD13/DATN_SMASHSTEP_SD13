package com.smashstep.datn.invoice.entity;

import com.smashstep.datn.customer.entity.KhachHang;
import com.smashstep.datn.employee.entity.NhanVien;
import com.smashstep.datn.promotion.entity.PhieuGiamGia;
import com.smashstep.datn.promotion.entity.PhieuGiamGiaCaNhan;
import com.smashstep.datn.workshift.entity.GiaoCa;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "hoa_don")
public class HoaDon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_khach_hang")
    private KhachHang idKhachHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nhan_vien")
    private NhanVien idNhanVien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_phieu_giam_gia")
    private PhieuGiamGia idPhieuGiamGia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_phieu_giam_gia_ca_nhan")
    private PhieuGiamGiaCaNhan idPhieuGiamGiaCaNhan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_giao_ca")
    private GiaoCa idGiaoCa;

    @Column(name = "ma_hoa_don")
    private String maHoaDon;

    @Column(name = "loai_don")
    private Integer loaiDon;

    @Column(name = "phi_van_chuyen")
    private BigDecimal phiVanChuyen;

    @Column(name = "tong_tien")
    private BigDecimal tongTien;

    @Column(name = "tong_tien_giam")
    private BigDecimal tongTienGiam;

    @Column(name = "tong_tien_sau_giam")
    private BigDecimal tongTienSauGiam;

    @Column(name = "ten_khach_hang")
    private String tenKhachHang;

    @Column(name = "dia_chi_khach_hang")
    private String diaChiKhachHang;

    @Column(name = "so_dien_thoai_khach_hang")
    private String soDienThoaiKhachHang;

    @Column(name = "email_khach_hang")
    private String emailKhachHang;

    @Column(name = "trang_thai_hien_tai")
    private Integer trangThaiHienTai;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;

    @Column(name = "nguoi_tao")
    private Long nguoiTao;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    @Column(name = "nguoi_cap_nhat")
    private Long nguoiCapNhat;

    @Column(name = "ngay_thanh_toan")
    private LocalDateTime ngayThanhToan;

    @Column(name = "ghi_chu")
    private String ghiChu;

}