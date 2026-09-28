package com.smashstep.datn.invoice.entity;

import com.smashstep.datn.employee.entity.NhanVien;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "lich_su_hoa_don")
public class LichSuHoaDon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_hoa_don")
    private HoaDon idHoaDon;

    @Column(name = "trang_thai")
    private Integer trangThai;

    @Column(name = "thoi_gian")
    private LocalDateTime thoiGian;

    @Column(name = "ghi_chu")
    private String ghiChu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_cap_nhat")
    private NhanVien nguoiCapNhat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_thuc_hien")
    private NhanVien nguoiThucHien;

}