package com.smashstep.datn.chat.entity;

import com.smashstep.datn.customer.entity.KhachHang;
import com.smashstep.datn.employee.entity.NhanVien;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "chat_phien")
public class ChatSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_khach_hang", nullable = false)
    private KhachHang customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nhan_vien")
    private NhanVien assignedEmployee;

    @Column(name = "trang_thai", nullable = false, length = 30)
    private String status;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "ngay_cap_nhat", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "ngay_yeu_cau_nhan_vien")
    private LocalDateTime requestedAt;

    @Column(name = "ngay_dong")
    private LocalDateTime closedAt;
}
