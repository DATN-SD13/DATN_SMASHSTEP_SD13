package com.smashstep.datn.invoice.dto;

import com.smashstep.datn.customer.entity.KhachHang;
import com.smashstep.datn.employee.entity.NhanVien;
import com.smashstep.datn.invoice.entity.HoaDon;
import com.smashstep.datn.invoice.enums.LoaiHoaDon;
import com.smashstep.datn.invoice.enums.HinhThucNhan;
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
    private String email;
    private String soDienThoai;
    private String diaChi;

    private Long idNhanVien;
    private String tenNhanVien;
    private String maNhanVien;

    private String loaiHoaDon;
    private Integer maLoaiHoaDon;
    private String hinhThucNhan;
    private Integer maHinhThucNhan;
    private String trangThai;
    private Integer maTrangThai;
    private Integer maTrangThaiTiepTheo;
    private String trangThaiTiepTheo;
    private String phuongThucThanhToan;
    private String trangThaiThanhToan;
    private Integer maTrangThaiThanhToan;
    private String ngayThanhToan;
    private String donViVanChuyen;
    private String maPhieuGiamGia;
    private Integer loaiGiamGia;
    private BigDecimal giaTriGiam;

    private BigDecimal tongTien;
    private BigDecimal tienGiamGia;
    private BigDecimal phiVanChuyen;
    private BigDecimal thanhTien;

    private String hoTenNguoiNhan;
    private String soDienThoaiNguoiNhan;
    private String diaChiGiaoHang;
    private String ghiChu;

    private List<HoaDonChiTietDto> chiTietHoaDon;
    private List<LichSuThanhToanDto> lichSuThanhToan;
    private List<LichSuHoaDonDto> lichSuHoaDon;

    public static HoaDonDetailDto from(HoaDon hoaDon, List<HoaDonChiTietDto> chiTietHoaDon) {
        return from(hoaDon, chiTietHoaDon, List.of(), List.of());
    }

    public static HoaDonDetailDto from(HoaDon hoaDon, List<HoaDonChiTietDto> chiTietHoaDon, List<LichSuThanhToanDto> lichSuThanhToan) {
        return from(hoaDon, chiTietHoaDon, lichSuThanhToan, List.of());
    }

    public static HoaDonDetailDto from(HoaDon hoaDon, List<HoaDonChiTietDto> chiTietHoaDon, List<LichSuThanhToanDto> lichSuThanhToan, List<LichSuHoaDonDto> lichSuHoaDon) {
        KhachHang khachHang = hoaDon.getIdKhachHang();
        NhanVien nhanVien = hoaDon.getIdNhanVien();
        PhuongThucThanhToan phuongThuc = hoaDon.getIdPhuongThucThanhToan();
        LoaiHoaDon loai = LoaiHoaDon.tuMa(hoaDon.getLoaiHoaDon());
        TrangThaiHoaDon trangThai = TrangThaiHoaDon.tuMa(hoaDon.getTrangThai());
        HinhThucNhan hinhThucNhan = HinhThucNhan.tuMa(hoaDon.getHinhThucNhan());
        if (hinhThucNhan == null) hinhThucNhan = hoaDon.getLoaiHoaDon() != null && hoaDon.getLoaiHoaDon() == 0 ? HinhThucNhan.NHAN_TAI_QUAY : HinhThucNhan.GIAO_HANG;
        TrangThaiHoaDon trangThaiTiepTheo = TrangThaiHoaDon.trangThaiTiepTheo(hoaDon.getTrangThai(), hinhThucNhan == HinhThucNhan.GIAO_HANG);

        return HoaDonDetailDto.builder()
                .id(hoaDon.getId())
                .maHoaDon(hoaDon.getMaHoaDon())
                .ngayTao(formatDateTime(hoaDon.getNgayTao()))
                .ngayCapNhat(formatDateTime(hoaDon.getNgayCapNhat()))
                .idKhachHang(khachHang != null ? khachHang.getId() : null)
                .tenKhachHang(khachHang != null ? khachHang.getTenKhachHang() : hoaDon.getHoTenNguoiNhan())
                .maKhachHang(khachHang != null ? khachHang.getMaKhachHang() : null)
                .email(khachHang != null ? khachHang.getEmail() : null)
                .soDienThoai(khachHang != null ? khachHang.getSoDienThoai() : hoaDon.getSoDienThoaiNguoiNhan())
                .diaChi(hoaDon.getDiaChiGiaoHang())
                .idNhanVien(nhanVien != null ? nhanVien.getId() : null)
                .tenNhanVien(nhanVien != null ? nhanVien.getTenNhanVien() : null)
                .maNhanVien(nhanVien != null ? nhanVien.getMaNhanVien() : null)
                .loaiHoaDon(loai != null ? loai.getNhan() : null)
                .maLoaiHoaDon(hoaDon.getLoaiHoaDon())
                .hinhThucNhan(hinhThucNhan.getNhan())
                .maHinhThucNhan(hinhThucNhan.getMa())
                .trangThai(trangThai != null ? trangThai.getNhan() : "Không xác định")
                .maTrangThai(hoaDon.getTrangThai())
                .maTrangThaiTiepTheo(trangThaiTiepTheo != null ? trangThaiTiepTheo.getMa() : null)
                .trangThaiTiepTheo(trangThaiTiepTheo != null ? trangThaiTiepTheo.getNhan() : null)
                .phuongThucThanhToan(phuongThuc != null ? phuongThuc.getTenPhuongThuc() : null)
                .trangThaiThanhToan(hoaDon.getNgayThanhToan() != null ? "Đã thanh toán" : "Chưa thanh toán")
                .maTrangThaiThanhToan(hoaDon.getNgayThanhToan() != null ? 1 : 0)
                .ngayThanhToan(formatDateTime(hoaDon.getNgayThanhToan()))
                .donViVanChuyen(hoaDon.getDonViVanChuyen())
                .maPhieuGiamGia(hoaDon.getIdPhieuGiamGia() != null ? hoaDon.getIdPhieuGiamGia().getMaPhieuGiamGia() : null)
                .loaiGiamGia(hoaDon.getIdPhieuGiamGia() != null ? hoaDon.getIdPhieuGiamGia().getLoaiGiamGia() : null)
                .giaTriGiam(hoaDon.getIdPhieuGiamGia() != null ? zero(hoaDon.getIdPhieuGiamGia().getGiaTriGiam()) : BigDecimal.ZERO)
                .tongTien(zero(hoaDon.getTongTien()))
                .tienGiamGia(HoaDonAmounts.discount(hoaDon))
                .phiVanChuyen(zero(hoaDon.getPhiVanChuyen()))
                .thanhTien(zero(hoaDon.getThanhTien()))
                .hoTenNguoiNhan(hoaDon.getHoTenNguoiNhan())
                .soDienThoaiNguoiNhan(hoaDon.getSoDienThoaiNguoiNhan())
                .diaChiGiaoHang(hoaDon.getDiaChiGiaoHang())
                .ghiChu(hoaDon.getGhiChu())
                .chiTietHoaDon(chiTietHoaDon == null ? List.of() : chiTietHoaDon)
                .lichSuThanhToan(lichSuThanhToan == null ? List.of() : lichSuThanhToan)
                .lichSuHoaDon(lichSuHoaDon == null ? List.of() : lichSuHoaDon)
                .build();
    }

    private static String formatDateTime(java.time.LocalDateTime value) {
        return value == null ? null : value.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    private static BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
