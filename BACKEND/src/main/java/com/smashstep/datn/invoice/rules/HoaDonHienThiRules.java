package com.smashstep.datn.invoice.rules;

import com.smashstep.datn.invoice.entity.HoaDon;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.JoinType;

/** Publish POS drafts and prepaid online orders only after a receipt is recorded. COD remains visible. */
public final class HoaDonHienThiRules {
    private HoaDonHienThiRules() {}
    public static Predicate daPhatHanh(Root<HoaDon> invoice, CriteriaBuilder cb) {
        Predicate legacy = cb.or(cb.isNull(invoice.get("loaiHoaDon")), cb.notEqual(invoice.get("loaiHoaDon"), 0),
                cb.isNull(invoice.get("ghiChu")), cb.and(
                        cb.notEqual(cb.locate(invoice.get("ghiChu"), PosDraftRules.PREFIX), 1),
                        cb.notEqual(cb.locate(invoice.get("ghiChu"), PosDraftRules.DELIVERY_PREFIX), 1)));
        var method = invoice.join("idPhuongThucThanhToan", JoinType.LEFT);
        Predicate notPrepaidOnline = cb.or(cb.isNull(invoice.get("loaiHoaDon")),
                cb.notEqual(invoice.get("loaiHoaDon"), 1), cb.isNull(method.get("maPhuongThuc")),
                cb.notEqual(method.get("maPhuongThuc"), "CHUYEN_KHOAN"));
        return cb.and(cb.or(legacy, cb.isNotNull(invoice.get("ngayThanhToan"))),
                cb.or(notPrepaidOnline, cb.isNotNull(invoice.get("ngayThanhToan"))));
    }
    public static boolean choThanhToanOnline(HoaDon invoice) {
        return Integer.valueOf(1).equals(invoice.getLoaiHoaDon()) && invoice.getNgayThanhToan() == null
                && invoice.getIdPhuongThucThanhToan() != null
                && "CHUYEN_KHOAN".equals(invoice.getIdPhuongThucThanhToan().getMaPhuongThuc());
    }
}
