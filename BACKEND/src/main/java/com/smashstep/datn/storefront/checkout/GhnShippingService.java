package com.smashstep.datn.storefront.checkout;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.sales.dto.SalesDelivery;
import com.smashstep.datn.sales.service.GhnPhiVanChuyenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

/** Customer adapter for the same two-level GHN catalogue and preview used by POS. */
@Service
@RequiredArgsConstructor
public class GhnShippingService {
    private final GhnPhiVanChuyenService ghn;
    public record Area(String id, String name) {}
    public List<Area> areas(String level, Integer parent) {
        var rows = switch (level) {
            case "provinces" -> ghn.tinhThanh();
            case "wards" -> {
                if (parent == null || parent <= 0) throw AppException.badRequest("Vui lòng chọn tỉnh / thành phố trước");
                yield ghn.phuongXa(parent);
            }
            default -> throw AppException.badRequest("Địa chỉ GHN hiện dùng tỉnh / thành phố và phường / xã");
        };
        return rows.stream().map(a -> new Area(a.id(), a.ten())).toList();
    }
    public BigDecimal fee(CommerceDtos.Receiver r, int quantity, BigDecimal goods) {
        if (r == null || r.provinceId() == null || r.wardCode() == null || r.wardCode().isBlank())
            throw AppException.badRequest("Chọn tỉnh / thành phố và phường / xã theo GHN để tính phí giao hàng");
        if (r.district() != null && !r.district().isBlank())
            throw AppException.badRequest("Vui lòng chọn lại địa chỉ GHN hai cấp, không dùng quận / huyện cũ");
        if (r.address().length() > 500) throw AppException.badRequest("Địa chỉ nhận hàng tối đa 500 ký tự");
        boolean provinceMatches = areas("provinces", null).stream()
                .anyMatch(a -> a.id().equals(r.provinceId().toString()) && a.name().equals(r.province()));
        boolean wardMatches = areas("wards", r.provinceId()).stream()
                .anyMatch(a -> a.id().equals(r.wardCode()) && a.name().equals(r.ward()));
        if (!provinceMatches || !wardMatches)
            throw AppException.badRequest("Tên tỉnh / thành phố, phường / xã không khớp mã GHN. Vui lòng chọn lại.");
        var delivery = new SalesDelivery(true, null, r.name(), r.phone(), r.address(), null);
        return ghn.tinhPhiKhachHang(delivery, quantity, r.provinceId(), r.wardCode()).phiVanChuyen();
    }
}
