package com.smashstep.datn.schedule.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Dữ liệu trả về khi có ca sắp bắt đầu cần xác nhận nhận ca.
 * Bao gồm thông tin ca mới và doanh thu tổng hợp của ca trước.
 */
@Getter
@Builder
public class GiaoCaResponse {

    /** ID lịch làm việc cần xác nhận */
    private Long lichLamViecId;

    /** Tên ca sắp bắt đầu */
    private String tenCa;

    /** Giờ bắt đầu ca mới (HH:mm) */
    private String gioBatDau;

    /** Giờ kết thúc ca mới (HH:mm) */
    private String gioKetThuc;

    // ── Dữ liệu ca trước ──────────────────────────────────

    /** Tên ca trước (null nếu không có ca trước) */
    private String tenCaTruoc;

    /** Tổng tiền mặt của ca trước */
    private BigDecimal tienMatCaTruoc;

    /** Tổng tiền chuyển khoản của ca trước */
    private BigDecimal chuyenKhoanCaTruoc;

    // ── Dữ liệu đề xuất đầu ca mới ────────────────────────

    /** Tiền mặt đầu ca (= tiền mặt ca trước) */
    private BigDecimal tienMatDauCa;

    /** Tiền chuyển khoản đầu ca (= chuyển khoản ca trước) */
    private BigDecimal chuyenKhoanDauCa;

    /** Tổng tiền đầu ca (= tienMatDauCa + chuyenKhoanDauCa) */
    private BigDecimal tongTienDauCa;
}
