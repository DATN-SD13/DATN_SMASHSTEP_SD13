package com.smashstep.datn.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "co_giay")
public class CoGiay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_co_giay")
    private String maCoGiay;

    @Column(name = "ten_co_giay")
    private String tenCoGiay;

    @Column(name = "trang_thai")
    private Integer trangThai;

}
