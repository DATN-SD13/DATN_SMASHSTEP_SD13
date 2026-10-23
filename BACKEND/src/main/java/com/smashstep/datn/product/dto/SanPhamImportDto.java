package com.smashstep.datn.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

public final class SanPhamImportDto {
    private SanPhamImportDto() {}
    public record Dong(@Min(2) @Max(501) int soDong,
                       @NotNull @Size(min=16,max=16) List<@NotNull @Size(max=10000) String> giaTri) {}
    public record KiemTraDong(int soDong, List<String> giaTri, List<String> loi) {}
    public record XemTruoc(String maLo, List<String> cot, int tongDong, int soSanPham,
                           long hopLe, long coLoi, List<KiemTraDong> dong) {}
    public record XacNhan(@NotBlank @Pattern(regexp="[0-9a-fA-F-]{36}") String maLo,
                          @NotEmpty @Size(max=500) List<@NotNull @Valid Dong> dong) {}
    public record SanPhamDaTao(String nhom, Long id, String maSanPham, int soBienThe) {}
    public record KetQua(String maLo, LocalDateTime ngayTao, int soSanPham, int soBienThe,
                         List<SanPhamDaTao> sanPham) {}
}
