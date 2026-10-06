package com.smashstep.datn.invoice.dto;

import com.smashstep.datn.employee.entity.NhanVien;
import com.smashstep.datn.invoice.entity.LichSuHoaDon;
import com.smashstep.datn.invoice.enums.TrangThaiHoaDon;
import lombok.Builder;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
@Builder
public class LichSuHoaDonDto {

    private Long id;
    private Long idNhanVien;
    private String maNhanVien;
    private String tenNhanVien;
    private String vaiTro;
    private Integer maTrangThai;
    private String trangThai;
    private String ghiChu;
    private String ngayTao;

    public static LichSuHoaDonDto from(LichSuHoaDon lichSu) {
        return from(lichSu, null);
    }

    public static LichSuHoaDonDto from(LichSuHoaDon lichSu, NhanVien nhanVien) {
        TrangThaiHoaDon trangThai = TrangThaiHoaDon.tuMa(lichSu.getTrangThai());
        String vaiTro = null;
        if (nhanVien != null && nhanVien.getIdVaiTro() != null) {
            vaiTro = nhanVien.getIdVaiTro().getTenVaiTro();
        }

        return LichSuHoaDonDto.builder()
                .id(lichSu.getId())
                .idNhanVien(lichSu.getNguoiTao())
                .maNhanVien(nhanVien != null ? nhanVien.getMaNhanVien() : null)
                .tenNhanVien(nhanVien != null ? nhanVien.getTenNhanVien() : null)
                .vaiTro(vaiTro)
                .maTrangThai(lichSu.getTrangThai())
                .trangThai(trangThai != null ? trangThai.getNhan() : "Không xác định")
                .ghiChu(lichSu.getGhiChu())
                .ngayTao(lichSu.getNgayTao() == null ? null : lichSu.getNgayTao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
                .build();
    }
}
