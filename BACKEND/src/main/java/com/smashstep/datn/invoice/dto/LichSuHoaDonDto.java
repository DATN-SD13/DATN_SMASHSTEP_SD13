package com.smashstep.datn.invoice.dto;

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
    private Integer maTrangThai;
    private String trangThai;
    private String ghiChu;
    private String ngayTao;

    public static LichSuHoaDonDto from(LichSuHoaDon lichSu) {
        TrangThaiHoaDon trangThai = TrangThaiHoaDon.tuMa(lichSu.getTrangThai());

        return LichSuHoaDonDto.builder()
                .id(lichSu.getId())
                .idNhanVien(lichSu.getNguoiTao())
                .maTrangThai(lichSu.getTrangThai())
                .trangThai(trangThai != null ? trangThai.getNhan() : "Không xác định")
                .ghiChu(lichSu.getGhiChu())
                .ngayTao(lichSu.getNgayTao() == null ? null : lichSu.getNgayTao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
                .build();
    }
}
