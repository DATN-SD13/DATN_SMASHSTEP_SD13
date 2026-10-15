package com.smashstep.datn.sales.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonSetter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class SalesRequest {
    @NotBlank
    @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    @JsonAlias("requestId")
    private String maYeuCau;

    @Min(2)
    @Max(2)
    private Integer phienBanThanhToan;

    @Positive
    @JsonAlias("customerId")
    private Long idKhachHang;

    @JsonAlias("employeeId")
    private Long idNhanVien;

    @Positive
    @JsonAlias("paymentMethodId")
    private Long idPhuongThucThanhToan;

    @Positive
    @JsonAlias("invoiceId")
    private Long idHoaDon;

    @Positive
    @JsonAlias("revision")
    private Long phienBan;

    @Valid
    @JsonAlias("delivery")
    private SalesDelivery giaoHang;

    @Size(max = 2)
    @Valid
    @JsonAlias("payments")
    private List<@NotNull Payment> danhSachThanhToan;

    @JsonAlias("transferConfirmed")
    private Boolean daXacNhanChuyenKhoan;

    @Size(max = 50)
    @JsonAlias("voucherCode")
    private String maPhieuGiamGia;

    @DecimalMin("0")
    @Digits(integer = 16, fraction = 2)
    @JsonAlias("paidAmount")
    private BigDecimal tienKhachDua;

    @Size(max = 1000)
    @JsonAlias("note")
    private String ghiChu;

    @NotEmpty
    @Size(max = 50)
    @Valid
    @JsonAlias("items")
    private List<@NotNull Item> danhSachSanPham;

    @Getter
    @Setter
    public static class Item {
        @NotNull
        @Positive
        @JsonAlias("variantId")
        private Long idSanPhamChiTiet;

        @NotNull
        @Min(1)
        @Max(10000)
        private Integer soLuong;

        @NotNull
        @DecimalMin("0")
        @Digits(integer = 16, fraction = 2)
        @JsonAlias("unitPrice")
        private BigDecimal donGia;

        @JsonSetter("soLuong")
        @JsonAlias("quantity")
        public void docSoLuong(BigDecimal giaTri) {
            try {
                soLuong = giaTri == null ? null : giaTri.intValueExact();
            } catch (ArithmeticException loi) {
                throw new IllegalArgumentException("Số lượng phải là số nguyên", loi);
            }
        }
    }

    @Getter
    @Setter
    public static class Payment {
        @NotNull
        @Positive
        @JsonAlias("methodId")
        private Long idPhuongThucThanhToan;

        @NotNull
        @DecimalMin("0")
        @Digits(integer = 16, fraction = 2)
        @JsonAlias("amount")
        private BigDecimal soTien;
    }
}
