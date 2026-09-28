package com.smashstep.datn.workshift.entity;

import com.smashstep.datn.employee.entity.NhanVien;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "lich_lam_viec_nhan_vien")
public class LichLamViecNhanVien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_lich_lam_viec")
    private LichLamViec idLichLamViec;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nhan_vien")
    private NhanVien idNhanVien;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;

    @Column(name = "nguoi_tao")
    private Long nguoiTao;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    @Column(name = "nguoi_cap_nhat")
    private Long nguoiCapNhat;

}