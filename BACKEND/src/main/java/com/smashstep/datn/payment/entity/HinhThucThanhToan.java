package com.smashstep.datn.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "hinh_thuc_thanh_toan")
public class HinhThucThanhToan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_hinh_thuc")
    private String maHinhThuc;

    @Column(name = "ten_hinh_thuc")
    private String tenHinhThuc;

    @Column(name = "trang_thai")
    private Integer trangThai;

}
