package com.smashstep.datn.invoice.rules;

import com.smashstep.datn.invoice.entity.HoaDon;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/** Keep unpaid managed POS drafts out of invoice lists and their aggregate counts. */
public final class HoaDonHienThiRules {
    private HoaDonHienThiRules() {}
    public static Predicate daPhatHanh(Root<HoaDon> invoice, CriteriaBuilder cb) {
        Predicate legacy = cb.or(cb.isNull(invoice.get("loaiHoaDon")), cb.notEqual(invoice.get("loaiHoaDon"), 0),
                cb.isNull(invoice.get("ghiChu")), cb.and(
                        cb.notEqual(cb.locate(invoice.get("ghiChu"), PosDraftRules.PREFIX), 1),
                        cb.notEqual(cb.locate(invoice.get("ghiChu"), PosDraftRules.DELIVERY_PREFIX), 1)));
        return cb.or(legacy, cb.isNotNull(invoice.get("ngayThanhToan")));
    }
}
