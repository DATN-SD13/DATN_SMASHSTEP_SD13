package com.smashstep.datn.invoice.dto;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.*;
@Getter @Setter
public class XuLyGiaoThatBaiDto {
    @NotBlank @Pattern(regexp="HANG_TRA|HOAN_TIEN|HUY") private String thaoTac;
    @NotNull private Integer soLanThatBai;
    private boolean daNhanLaiHang;
    private boolean nhapLaiKho;
    private boolean daHoanTien;
    @DecimalMin("0") @Digits(integer=16,fraction=2) private java.math.BigDecimal soTienHoan;
    @Size(max=100, message="Mã giao dịch/chứng từ tối đa 100 ký tự") private String maHoanTien;
    @NotBlank @Size(max=700) private String ghiChu;
}
