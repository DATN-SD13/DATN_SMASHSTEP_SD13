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
    private String tenKhachHang;
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

        var invoice = lichSu.getIdHoaDon();
        String note = lichSu.getGhiChu();
        // Chỉ nhận các sự kiện cố định do checkout phát sinh, không gán thao tác hệ thống/nhân viên cho khách.
        boolean customerAction = lichSu.getNguoiTao() == null && invoice != null
                && Integer.valueOf(1).equals(invoice.getLoaiHoaDon()) && invoice.getIdKhachHang() != null
                && ("Khách hàng đặt hàng online; chờ xác nhận và thu COD".equals(note)
                || "Khách hàng đặt hàng online; chờ chuyển khoản QR và cửa hàng đối soát".equals(note)
                || "Khách hàng cập nhật thông tin nhận hàng (1/1)".equals(note)
                || "Khách hàng hủy đơn khi đang chờ xác nhận".equals(note)
                || "Đặt hàng online, đã thanh toán bằng VNPAY (Sandbox); chờ cửa hàng xác nhận".equals(note));
        String customerName = customerAction ? invoice.getIdKhachHang().getTenKhachHang() : null;

        return LichSuHoaDonDto.builder()
                .id(lichSu.getId())
                .idNhanVien(lichSu.getNguoiTao())
                .maNhanVien(nhanVien != null ? nhanVien.getMaNhanVien() : null)
                .tenNhanVien(nhanVien != null ? nhanVien.getTenNhanVien() : null)
                .tenKhachHang(customerAction ? (customerName == null || customerName.isBlank() ? "Khách hàng" : customerName) : null)
                .vaiTro(customerAction ? "Khách hàng" : vaiTro)
                .maTrangThai(lichSu.getTrangThai())
                .trangThai(com.smashstep.datn.invoice.rules.ReturnedGoodsRules.reserved(lichSu.getGhiChu()) ? "Đã xử lý hàng trả" : trangThai != null ? trangThai.getNhan() : "Không xác định")
                .ghiChu(
                        com.smashstep.datn.invoice.rules.PosHistoryRules.noiDung(
                                com.smashstep.datn.invoice.rules.ReturnedGoodsRules.note(lichSu.getGhiChu())))
                .ngayTao(
                        lichSu.getNgayTao() == null
                                ? null
                                : lichSu.getNgayTao()
                                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
                .build();
    }
}
