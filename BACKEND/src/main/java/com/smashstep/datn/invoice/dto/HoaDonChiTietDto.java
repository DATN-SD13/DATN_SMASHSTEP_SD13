package com.smashstep.datn.invoice.dto;

import com.smashstep.datn.invoice.entity.HoaDonChiTiet;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class HoaDonChiTietDto {
    private Long id;
    private Long idSanPhamChiTiet;
    private String maSanPhamChiTiet;
    private String tenSanPham;
    private String maSku;
    private String mauSac;
    private String kichThuoc;
    private Integer soLuong;
    private BigDecimal donGia;
    private BigDecimal thanhTien;

    public static HoaDonChiTietDto from(HoaDonChiTiet chiTiet) {
        String tenSanPham = null;
        String mauSac = null;
        String kichThuoc = null;
        String maSku = null;
        String maChiTiet = null;
        Long idSanPhamChiTiet = null;

        if (chiTiet.getIdSanPhamChiTiet() != null) {
            idSanPhamChiTiet = chiTiet.getIdSanPhamChiTiet().getId();
            maChiTiet = chiTiet.getIdSanPhamChiTiet().getMaChiTietSanPham();
            maSku = chiTiet.getIdSanPhamChiTiet().getSku();

            if (chiTiet.getIdSanPhamChiTiet().getIdSanPham() != null) {
                tenSanPham = chiTiet.getIdSanPhamChiTiet().getIdSanPham().getTenSanPham();
            }
            if (chiTiet.getIdSanPhamChiTiet().getIdMauSac() != null) {
                mauSac = chiTiet.getIdSanPhamChiTiet().getIdMauSac().getTenMauSac();
            }
            if (chiTiet.getIdSanPhamChiTiet().getIdKichThuoc() != null) {
                kichThuoc = chiTiet.getIdSanPhamChiTiet().getIdKichThuoc().getGiaTri();
            }
        }

        return HoaDonChiTietDto.builder()
                .id(chiTiet.getId())
                .idSanPhamChiTiet(idSanPhamChiTiet)
                .maSanPhamChiTiet(maChiTiet)
                .tenSanPham(tenSanPham)
                .maSku(maSku)
                .mauSac(mauSac)
                .kichThuoc(kichThuoc)
                .soLuong(chiTiet.getSoLuong())
                .donGia(chiTiet.getDonGia())
                .thanhTien(chiTiet.getThanhTien())
                .build();
    }
}
