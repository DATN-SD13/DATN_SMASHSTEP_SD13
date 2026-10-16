package com.smashstep.datn.sales.service;

import com.smashstep.datn.invoice.rules.PosDraftRules;

import jakarta.persistence.EntityManager;

import java.util.*;

/** Chỉ các giỏ cũ chưa trừ kho thật mới phải trừ khỏi tồn khả dụng. */
public final class GiuHangTaiQuay {
    private GiuHangTaiQuay() {}

    public static Map<Long, Long> daGiu(EntityManager em, Collection<Long> ids, Long boQuaHoaDon) {
        if (ids.isEmpty()) return Map.of();
        var rows =
                em.createQuery(
                                "select d.idSanPhamChiTiet.id, sum(d.soLuong) from HoaDonChiTiet d"
                                        + " join d.idHoaDon h where h.loaiHoaDon = 0 and h.trangThai ="
                                        + " 8 and h.ngayThanhToan is null and (locate(:pickup,"
                                        + " h.ghiChu) = 1 or locate(:delivery, h.ghiChu) = 1) and not"
                                        + " (locate(:pickupStock, h.ghiChu) = 1 or locate(:deliveryStock, h.ghiChu) = 1) and not"
                                        + " exists (select p.id from LichSuThanhToan p where p.idHoaDon"
                                        + " = h) and d.soLuong > 0 and d.idSanPhamChiTiet.id in :ids"
                                        + " and (:boQua is null or h.id <> :boQua) group by"
                                        + " d.idSanPhamChiTiet.id",
                                Object[].class)
                        .setParameter("pickup", PosDraftRules.PREFIX)
                        .setParameter("delivery", PosDraftRules.DELIVERY_PREFIX)
                        .setParameter("pickupStock", PosDraftRules.PREFIX + PosDraftRules.TON_KHO_THUC)
                        .setParameter("deliveryStock", PosDraftRules.DELIVERY_PREFIX + PosDraftRules.TON_KHO_THUC)
                        .setParameter("ids", ids)
                        .setParameter("boQua", boQuaHoaDon)
                        .getResultList();
        Map<Long, Long> result = new HashMap<>();
        for (var row : rows)
            result.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        return result;
    }

    public static int khaDung(Integer thucTe, long daGiu) {
        return (int) Math.max(0, (long) (thucTe == null ? 0 : thucTe) - daGiu);
    }

    public static boolean choPhepLuu(int moi, int cu, int khaDung) {
        return moi <= khaDung || moi <= cu;
    }
}
