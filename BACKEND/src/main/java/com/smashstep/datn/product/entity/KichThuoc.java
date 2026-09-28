package com.smashstep.datn.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "kich_thuoc")
public class KichThuoc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_kich_thuoc")
    private String maKichThuoc;

    @Column(name = "ten_kich_thuoc")
    private String tenKichThuoc;

    @Column(name = "size")
    private String size;

    @Column(name = "trang_thai")
    private Integer trangThai;

    @Column(name = "xoa_mem")
    private Boolean xoaMem;

}