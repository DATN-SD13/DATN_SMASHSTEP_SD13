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

@Getter
@Builder
public class HoaDonListDto {

    private final Long id;
    private final String maHoaDon;
    private final String tenNhanVien;
    private final String maNhanVien;
    private final String tenKhachHang;
    private final String soDienThoai;
    private final String ngayTao;
    private final BigDecimal tongTien;
    private final BigDecimal tienGiamGia;
    private final BigDecimal phiVanChuyen;
    private final BigDecimal thanhTien;
    private final String loaiHoaDon;
    private final Integer maLoaiHoaDon;
    private final String trangThai;
    private final String lopTrangThai;
    private final Integer maTrangThai;
    private final String phuongThucThanhToan;
    private final String diaChi;

    public static HoaDonListDto from(HoaDon h) {
        KhachHang kh = h.getIdKhachHang();
        NhanVien nv = h.getIdNhanVien();
        PhuongThucThanhToan pt = h.getIdPhuongThucThanhToan();
        TrangThaiHoaDon tt = TrangThaiHoaDon.tuMa(h.getTrangThai());
        LoaiHoaDon loai = LoaiHoaDon.tuMa(h.getLoaiHoaDon());

        return HoaDonListDto.builder()
                .id(h.getId())
                .maHoaDon(h.getMaHoaDon())
                .tenNhanVien(nv != null ? nv.getTenNhanVien() : null)
                .maNhanVien(nv != null ? nv.getMaNhanVien() : null)
                .tenKhachHang(firstNotBlank(kh != null ? kh.getTenKhachHang() : null, h.getHoTenNguoiNhan(), "Khách lẻ"))
                .soDienThoai(firstNotBlank(h.getSoDienThoaiNguoiNhan(), kh != null ? kh.getSoDienThoai() : null, ""))
                .ngayTao(h.getNgayTao() != null ? h.getNgayTao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : null)
                .tongTien(nz(h.getTongTien()))
                .tienGiamGia(nz(h.getTienGiamGia()))
                .phiVanChuyen(nz(h.getPhiVanChuyen()))
                .thanhTien(nz(h.getThanhTien()))
                .loaiHoaDon(loai != null ? loai.getKhoa() : null)
                .maLoaiHoaDon(h.getLoaiHoaDon())
                .trangThai(tt != null ? tt.getNhan() : "Không xác định")
                .lopTrangThai(tt != null ? tt.getLop() : "unknown")
                .maTrangThai(h.getTrangThai())
                .phuongThucThanhToan(pt != null ? pt.getTenPhuongThuc() : null)
                .diaChi(h.getDiaChiGiaoHang())
                .build();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static String firstNotBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
