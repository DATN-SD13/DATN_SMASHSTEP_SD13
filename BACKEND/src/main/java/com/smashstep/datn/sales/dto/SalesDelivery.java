package com.smashstep.datn.sales.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record SalesDelivery(
        @JsonAlias("enabled") boolean apDung,
        @Positive @JsonAlias("addressId") Long idDiaChi,
        @Size(max = 255) @JsonAlias("recipientName") String tenNguoiNhan,
        @Size(max = 20) @JsonAlias("recipientPhone") String soDienThoaiNguoiNhan,
        @Size(max = 500) @JsonAlias("address") String diaChi,
        @DecimalMin("0") @Digits(integer = 16, fraction = 2) @JsonAlias("shippingFee")
        BigDecimal phiVanChuyen) {}
