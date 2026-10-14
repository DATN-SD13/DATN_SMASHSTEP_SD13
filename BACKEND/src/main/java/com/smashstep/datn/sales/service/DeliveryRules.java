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
    public static SalesDelivery resolve(EntityManager em, Long customerId, SalesDelivery input) {
        if (input == null || !input.enabled()) return new SalesDelivery(false, null, null, null, null, BigDecimal.ZERO);
        String name = input.recipientName(), phone = input.recipientPhone(), address = input.address();
        if (input.addressId() != null) {
            var saved = em.find(DiaChiKhachHang.class, input.addressId());
            if (saved == null || saved.getIdKhachHang() == null || !Objects.equals(saved.getIdKhachHang().getId(), customerId)
                    || !Integer.valueOf(1).equals(saved.getTrangThai()))
                throw AppException.badRequest("Địa chỉ không thuộc khách hàng hoặc không hoạt động");
            name = saved.getTenNguoiNhan(); phone = saved.getSdtNguoiNhan();
            address = Stream.of(saved.getDiaChiCuThe(), saved.getPhuongXa(), saved.getQuanHuyen(), saved.getTinhThanh())
                    .filter(v -> v != null && !v.isBlank()).reduce((a,b) -> a + ", " + b).orElse("");
        }
        if (name == null || name.isBlank() || name.length() > 255 || phone == null || !phone.matches("0[0-9]{9}")
                || address == null || address.isBlank() || address.length() > 500)
            throw AppException.badRequest("Giao hàng cần tên người nhận, số điện thoại 10 số bắt đầu bằng 0 và địa chỉ đầy đủ");
        BigDecimal fee = input.shippingFee();
        if (fee == null || fee.signum() < 0 || fee.scale() > 2 || fee.precision() - fee.scale() > 16)
            throw AppException.badRequest("Phí vận chuyển không hợp lệ");
        return new SalesDelivery(true, null, name.trim(), phone, address.trim(), fee);
    }
    public static SalesDelivery from(HoaDon h) {
        return new SalesDelivery(PosDraftRules.delivery(h), null, h.getHoTenNguoiNhan(), h.getSoDienThoaiNguoiNhan(),
                h.getDiaChiGiaoHang(), h.getPhiVanChuyen() == null ? BigDecimal.ZERO : h.getPhiVanChuyen());
    }
    public static void apply(HoaDon h, SalesDelivery delivery, String note) {
        h.setGhiChu((delivery.enabled() ? PosDraftRules.DELIVERY_PREFIX : PosDraftRules.PREFIX) + Objects.toString(note, ""));
        h.setPhiVanChuyen(delivery.shippingFee()); h.setDonViVanChuyen(null);
        h.setDiaChiGiaoHang(delivery.enabled() ? delivery.address() : null);
        if (delivery.enabled()) { h.setHoTenNguoiNhan(delivery.recipientName()); h.setSoDienThoaiNguoiNhan(delivery.recipientPhone()); }
    }
    public static boolean same(HoaDon h, SalesDelivery delivery) {
        if (PosDraftRules.delivery(h) != delivery.enabled()) return false;
        if (!delivery.enabled()) return true;
        return Objects.equals(h.getHoTenNguoiNhan(), delivery.recipientName())
                && Objects.equals(h.getSoDienThoaiNguoiNhan(), delivery.recipientPhone())
                && Objects.equals(h.getDiaChiGiaoHang(), delivery.address())
                && h.getPhiVanChuyen().compareTo(delivery.shippingFee()) == 0;
    }
}
