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
    private String tenDanhMuc;
    private String tenThuongHieu;
    private String tenChatLieu;
    private String tenKieuDang;
    private String tenCoGiay;
    private String tenXuatXu;
    private Integer soLuong;
    private BigDecimal donGia;
    private BigDecimal thanhTien;

    public static HoaDonChiTietDto from(HoaDonChiTiet chiTiet) {
        String tenSanPham = null;
        String mauSac = null;
        String kichThuoc = null;
        String maSku = null;
        String maChiTiet = null;
        String tenDanhMuc = null;
        String tenThuongHieu = null;
        String tenChatLieu = null;
        String tenKieuDang = null;
        String tenCoGiay = null;
        String tenXuatXu = null;
        Long idSanPhamChiTiet = null;

        if (chiTiet.getIdSanPhamChiTiet() != null) {
            idSanPhamChiTiet = chiTiet.getIdSanPhamChiTiet().getId();
            maChiTiet = chiTiet.getIdSanPhamChiTiet().getMaChiTietSanPham();
            maSku = chiTiet.getIdSanPhamChiTiet().getSku();

            if (chiTiet.getIdSanPhamChiTiet().getIdSanPham() != null) {
                var sanPham = chiTiet.getIdSanPhamChiTiet().getIdSanPham();
                tenSanPham = sanPham.getTenSanPham();
                if (sanPham.getIdDanhMuc() != null) {
                    tenDanhMuc = sanPham.getIdDanhMuc().getTenDanhMuc();
                }
                if (sanPham.getIdThuongHieu() != null) {
                    tenThuongHieu = sanPham.getIdThuongHieu().getTenThuongHieu();
                }
                if (sanPham.getIdChatLieu() != null) {
                    tenChatLieu = sanPham.getIdChatLieu().getTenChatLieu();
                }
                if (sanPham.getIdKieuDang() != null) {
                    tenKieuDang = sanPham.getIdKieuDang().getTenKieuDang();
                }
                if (sanPham.getIdCoGiay() != null) {
                    tenCoGiay = sanPham.getIdCoGiay().getTenCoGiay();
                }
                if (sanPham.getIdXuatXu() != null) {
                    tenXuatXu = sanPham.getIdXuatXu().getTenXuatXu();
                }
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
                .tenDanhMuc(tenDanhMuc)
                .tenThuongHieu(tenThuongHieu)
                .tenChatLieu(tenChatLieu)
                .tenKieuDang(tenKieuDang)
                .tenCoGiay(tenCoGiay)
                .tenXuatXu(tenXuatXu)
                .soLuong(chiTiet.getSoLuong())
                .donGia(chiTiet.getDonGia())
                .thanhTien(chiTiet.getThanhTien())
                .build();
    }
}
