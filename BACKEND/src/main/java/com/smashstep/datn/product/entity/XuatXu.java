package com.smashstep.datn.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "xuat_xu")
public class XuatXu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_xuat_xu")
    private String maXuatXu;

    @Column(name = "ten_xuat_xu")
    private String tenXuatXu;

    @Column(name = "trang_thai")
    private Integer trangThai;

}
