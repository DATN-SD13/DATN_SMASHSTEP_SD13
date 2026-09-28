package com.smashstep.datn.employee.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "quyen_han")
public class QuyenHan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_quyen_han")
    private String maQuyenHan;

    @Column(name = "ten_quyen_han")
    private String tenQuyenHan;

    @Column(name = "trang_thai")
    private Integer trangThai;

    @Column(name = "xoa_mem")
    private Boolean xoaMem;

}