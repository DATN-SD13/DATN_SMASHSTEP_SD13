package com.smashstep.datn.employee.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "quyen_han_chuc_nang")
public class QuyenHanChucNang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_quyen_han")
    private QuyenHan idQuyenHan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_chuc_nang")
    private ChucNang idChucNang;

    @Column(name = "trang_thai")
    private Integer trangThai;

    @Column(name = "xoa_mem")
    private Boolean xoaMem;

}