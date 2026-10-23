package com.smashstep.datn.schedule.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public final class LichLamViecImportDto {
    private LichLamViecImportDto() {}
    public record Dong(@Min(2) @Max(5001) int soDong,
                       @NotNull @Size(max=10000) String maNhanVien,
                       @NotNull @Size(max=10000) String maCa,
                       @NotNull @Size(max=10000) String ngayLam,
                       @NotNull @Size(max=10000) String ghiChu) {}
    public record KiemTraDong(Dong duLieu, String tenNhanVien, String tenCa,
                              boolean daTonTai, List<String> loi) {}
    public record XemTruoc(int tongDong, long hopLe, long daTonTai, long coLoi,
                           List<KiemTraDong> dong) {}
    public record XacNhan(@NotEmpty @Size(max=500) List<@NotNull @Valid Dong> dong) {}
    public record KetQua(int daTao, int boQua, int loi, String thongBao) {}
}
