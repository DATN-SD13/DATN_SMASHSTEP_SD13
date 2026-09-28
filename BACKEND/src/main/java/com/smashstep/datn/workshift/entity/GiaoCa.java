package com.smashstep.datn.workshift.entity;

import com.smashstep.datn.employee.entity.NhanVien;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "giao_ca")
public class GiaoCa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_giao_ca")
    private String maGiaoCa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_lich_lam_viec")
    private LichLamViec idLichLamViec;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nhan_vien")
    private NhanVien idNhanVien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_giao_ca_truoc")
    private GiaoCa idGiaoCaTruoc;

    @Column(name = "thoi_gian_nhan_ca")
    private LocalDateTime thoiGianNhanCa;

    @Column(name = "thoi_gian_ket_thuc")
    private LocalDateTime thoiGianKetThuc;

    @Column(name = "tien_ban_giao")
    private BigDecimal tienBanGiao;

    @Column(name = "xac_nhan")
    private Boolean xacNhan;

    @Column(name = "trang_thai")
    private Integer trangThai;

    @Column(name = "ghi_chu")
    private String ghiChu;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;

    @Column(name = "nguoi_tao")
    private Long nguoiTao;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    @Column(name = "nguoi_cap_nhat")
    private Long nguoiCapNhat;

}