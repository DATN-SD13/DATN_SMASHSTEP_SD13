package com.smashstep.datn.sales.service;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.customer.entity.DiaChiKhachHang;
import com.smashstep.datn.invoice.entity.HoaDon;
import com.smashstep.datn.invoice.rules.PosDraftRules;
import com.smashstep.datn.sales.dto.SalesDelivery;

import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.stream.Stream;

public final class DeliveryRules {
    private DeliveryRules() {}

    public static SalesDelivery chuanHoa(
            EntityManager entityManager, Long idKhachHang, SalesDelivery duLieuNhap) {
        if (duLieuNhap == null || !duLieuNhap.apDung())
            return new SalesDelivery(false, null, null, null, null, BigDecimal.ZERO);
        String ten = duLieuNhap.tenNguoiNhan(),
                soDienThoai = duLieuNhap.soDienThoaiNguoiNhan(),
                diaChi = duLieuNhap.diaChi();
        if (duLieuNhap.idDiaChi() != null) {
            var daLuu = entityManager.find(DiaChiKhachHang.class, duLieuNhap.idDiaChi());
            if (daLuu == null
                    || daLuu.getIdKhachHang() == null
                    || !Objects.equals(daLuu.getIdKhachHang().getId(), idKhachHang)
                    || !Integer.valueOf(1).equals(daLuu.getTrangThai()))
                throw AppException.badRequest(
                        "Địa chỉ không thuộc khách hàng hoặc không hoạt động");
            ten = daLuu.getTenNguoiNhan();
            soDienThoai = daLuu.getSdtNguoiNhan();
            diaChi =
                    Stream.of(
                                    daLuu.getDiaChiCuThe(),
                                    daLuu.getPhuongXa(),
                                    daLuu.getQuanHuyen(),
                                    daLuu.getTinhThanh())
                            .filter(v -> v != null && !v.isBlank())
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("");
        }
        if (ten == null
                || ten.isBlank()
                || ten.length() > 255
                || soDienThoai == null
                || !soDienThoai.matches("0[0-9]{9}")
                || diaChi == null
                || diaChi.isBlank()
                || diaChi.length() > 500)
            throw AppException.badRequest(
                    "Giao hàng cần tên người nhận, số điện thoại 10 số bắt đầu bằng 0 và địa chỉ"
                            + " đầy đủ");
        BigDecimal phi = duLieuNhap.phiVanChuyen();
        if (phi == null
                || phi.signum() < 0
                || phi.scale() > 2
                || phi.precision() - phi.scale() > 16)
            throw AppException.badRequest("Phí vận chuyển không hợp lệ");
        return new SalesDelivery(true, null, ten.trim(), soDienThoai, diaChi.trim(), phi);
    }

    public static SalesDelivery tuHoaDon(HoaDon h) {
        return new SalesDelivery(
                PosDraftRules.delivery(h),
                null,
                h.getHoTenNguoiNhan(),
                h.getSoDienThoaiNguoiNhan(),
                h.getDiaChiGiaoHang(),
                h.getPhiVanChuyen() == null ? BigDecimal.ZERO : h.getPhiVanChuyen());
    }

    public static void apDung(HoaDon h, SalesDelivery giaoHang, String ghiChu) {
        h.setGhiChu(
                (giaoHang.apDung() ? PosDraftRules.DELIVERY_PREFIX : PosDraftRules.PREFIX)
                        + Objects.toString(ghiChu, ""));
        h.setPhiVanChuyen(giaoHang.phiVanChuyen());
        h.setDonViVanChuyen(null);
        h.setDiaChiGiaoHang(giaoHang.apDung() ? giaoHang.diaChi() : null);
        if (giaoHang.apDung()) {
            h.setHoTenNguoiNhan(giaoHang.tenNguoiNhan());
            h.setSoDienThoaiNguoiNhan(giaoHang.soDienThoaiNguoiNhan());
        }
    }

    public static boolean khopHoaDon(HoaDon h, SalesDelivery giaoHang) {
        if (PosDraftRules.delivery(h) != giaoHang.apDung()) return false;
        if (!giaoHang.apDung()) return true;
        return Objects.equals(h.getHoTenNguoiNhan(), giaoHang.tenNguoiNhan())
                && Objects.equals(h.getSoDienThoaiNguoiNhan(), giaoHang.soDienThoaiNguoiNhan())
                && Objects.equals(h.getDiaChiGiaoHang(), giaoHang.diaChi())
                && h.getPhiVanChuyen().compareTo(giaoHang.phiVanChuyen()) == 0;
    }
}
