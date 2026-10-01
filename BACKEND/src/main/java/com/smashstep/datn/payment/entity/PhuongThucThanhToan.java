package com.smashstep.datn.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "phuong_thuc_thanh_toan")
public class PhuongThucThanhToan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_hinh_thuc_thanh_toan")
    private HinhThucThanhToan idHinhThucThanhToan;

    @Column(name = "ma_phuong_thuc")
    private String maPhuongThuc;

    @Column(name = "ten_phuong_thuc")
    private String tenPhuongThuc;

    @Column(name = "trang_thai")
    private Integer trangThai;

}
