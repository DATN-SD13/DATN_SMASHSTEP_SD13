package com.smashstep.datn.sales.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record PendingSaleRequest(
        @NotNull @Positive @JsonAlias("revision") Long phienBan,
        @Positive @JsonAlias("customerId") Long idKhachHang,
        @Size(max = 980) @JsonAlias("note") String ghiChu,
        @NotNull @Size(max = 50) @JsonAlias("items") List<@NotNull @Valid Item> danhSachSanPham,
        @Valid @JsonAlias("delivery") SalesDelivery giaoHang,
        @Size(max = 50) @JsonAlias("voucherCode") String maPhieuGiamGia) {
    public PendingSaleRequest(
            Long phienBan, Long idKhachHang, String ghiChu, List<Item> danhSachSanPham) {
        this(phienBan, idKhachHang, ghiChu, danhSachSanPham, null, null);
    }

    public record Item(
            @NotNull @Positive @JsonAlias("variantId") Long idSanPhamChiTiet,
            @NotNull
            @DecimalMin("1")
            @DecimalMax("10000")
            @Digits(integer = 5, fraction = 0)
            @JsonAlias("quantity")
            BigDecimal soLuong,
            @NotNull @DecimalMin("0") @Digits(integer = 16, fraction = 2) @JsonAlias("unitPrice")
            BigDecimal donGia) {}
}
