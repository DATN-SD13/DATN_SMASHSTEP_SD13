package com.smashstep.datn.sales.service;

import com.smashstep.datn.sales.dto.SalesRequest;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * Persisted in payment descriptions, so retries do not depend on current prices or voucher
 * validity.
 */
public final class CheckoutFingerprint {
    private CheckoutFingerprint() {}

    public static String taoMa(SalesRequest r) {
        StringBuilder b = new StringBuilder();
        for (Object v :
                Arrays.asList(
                        r.getIdHoaDon(),
                        r.getPhienBan(),
                        r.getIdNhanVien(),
                        r.getIdKhachHang(),
                        Objects.toString(r.getGhiChu(), ""),
                        chuanHoaMa(r.getMaPhieuGiamGia()),
                        r.getTienKhachDua(),
                        Boolean.TRUE.equals(r.getDaXacNhanChuyenKhoan()))) themGiaTri(b, v);
        var giaoHang = r.getGiaoHang();
        themGiaTri(b, giaoHang != null && giaoHang.apDung());
        if (giaoHang != null && giaoHang.apDung())
            for (Object v :
                    Arrays.asList(
                            giaoHang.idDiaChi(),
                            giaoHang.tenNguoiNhan(),
                            giaoHang.soDienThoaiNguoiNhan(),
                            giaoHang.diaChi(),
                            giaoHang.phiVanChuyen())) themGiaTri(b, v);
        r.getDanhSachSanPham().stream()
                .sorted(Comparator.comparing(SalesRequest.Item::getIdSanPhamChiTiet))
                .forEach(
                        i -> {
                            themGiaTri(b, i.getIdSanPhamChiTiet());
                            themGiaTri(b, i.getSoLuong());
                            themGiaTri(b, i.getDonGia());
                        });
        for (var p : r.getDanhSachThanhToan()) {
            themGiaTri(b, p.getIdPhuongThucThanhToan());
            themGiaTri(b, p.getSoTien());
        }
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(b.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public static String chuanHoaMa(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    private static void themGiaTri(StringBuilder b, Object value) {
        String s =
                value instanceof BigDecimal m
                        ? m.stripTrailingZeros().toPlainString()
                        : Objects.toString(value, "");
        b.append(s.length()).append(':').append(s).append('|');
    }
}
