package com.smashstep.datn.invoice.dto;

import com.smashstep.datn.customer.entity.KhachHang;
import com.smashstep.datn.employee.entity.NhanVien;
import com.smashstep.datn.invoice.entity.HoaDon;
import com.smashstep.datn.invoice.enums.LoaiHoaDon;
import com.smashstep.datn.invoice.enums.TrangThaiHoaDon;
import com.smashstep.datn.payment.entity.PhuongThucThanhToan;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
@Builder
public class HoaDonDetailDto {
    private Long id;
    private String maHoaDon;
    private String ngayTao;
    private String ngayCapNhat;

    private Long idKhachHang;
    private String tenKhachHang;
    private String maKhachHang;
    private String soDienThoai;
    private String diaChi;

    private Long idNhanVien;
    private String tenNhanVien;
    private String maNhanVien;

    private String loaiHoaDon;
    private Integer maLoaiHoaDon;
    private String trangThai;
    private Integer maTrangThai;
    private String phuongThucThanhToan;

    private BigDecimal tongTien;
    private BigDecimal tienGiamGia;
    private BigDecimal phiVanChuyen;
    private BigDecimal thanhTien;

    private String hoTenNguoiNhan;
    private String soDienThoaiNguoiNhan;
    private String diaChiGiaoHang;
    private String ghiChu;

    private List<HoaDonChiTietDto> chiTietHoaDon;

    public static HoaDonDetailDto from(HoaDon hoaDon, List<HoaDonChiTietDto> chiTietHoaDon) {
        KhachHang khachHang = hoaDon.getIdKhachHang();
        NhanVien nhanVien = hoaDon.getIdNhanVien();
        PhuongThucThanhToan phuongThuc = hoaDon.getIdPhuongThucThanhToan();
        LoaiHoaDon loai = LoaiHoaDon.tuMa(hoaDon.getLoaiHoaDon());
        TrangThaiHoaDon trangThai = TrangThaiHoaDon.tuMa(hoaDon.getTrangThai());

        return HoaDonDetailDto.builder()
                .id(hoaDon.getId())
                .maHoaDon(hoaDon.getMaHoaDon())
                .ngayTao(formatDate(hoaDon.getNgayTao()))
                .ngayCapNhat(formatDate(hoaDon.getNgayCapNhat()))
                .idKhachHang(khachHang != null ? khachHang.getId() : null)
                .tenKhachHang(khachHang != null ? khachHang.getTenKhachHang() : hoaDon.getHoTenNguoiNhan())
                .maKhachHang(khachHang != null ? khachHang.getMaKhachHang() : null)
                .soDienThoai(khachHang != null ? khachHang.getSoDienThoai() : hoaDon.getSoDienThoaiNguoiNhan())
                .diaChi(khachHang != null ? null : hoaDon.getDiaChiGiaoHang())
                .idNhanVien(nhanVien != null ? nhanVien.getId() : null)
                .tenNhanVien(nhanVien != null ? nhanVien.getTenNhanVien() : null)
                .maNhanVien(nhanVien != null ? nhanVien.getMaNhanVien() : null)
                .loaiHoaDon(loai != null ? loai.getNhan() : null)
                .maLoaiHoaDon(hoaDon.getLoaiHoaDon())
                .trangThai(trangThai != null ? trangThai.getNhan() : "Không xác định")
                .maTrangThai(hoaDon.getTrangThai())
                .phuongThucThanhToan(phuongThuc != null ? phuongThuc.getTenPhuongThuc() : null)
                .tongTien(zero(hoaDon.getTongTien()))
                .tienGiamGia(zero(hoaDon.getTienGiamGia()))
                .phiVanChuyen(zero(hoaDon.getPhiVanChuyen()))
                .thanhTien(zero(hoaDon.getThanhTien()))
                .hoTenNguoiNhan(hoaDon.getHoTenNguoiNhan())
                .soDienThoaiNguoiNhan(hoaDon.getSoDienThoaiNguoiNhan())
                .diaChiGiaoHang(hoaDon.getDiaChiGiaoHang())
                .ghiChu(hoaDon.getGhiChu())
                .chiTietHoaDon(chiTietHoaDon)
                .build();
    }

    private static String formatDate(java.time.LocalDateTime value) {
        return value == null ? null : value.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private static BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
