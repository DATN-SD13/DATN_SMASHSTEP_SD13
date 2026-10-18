package com.smashstep.datn.invoice.dto;

import com.smashstep.datn.invoice.entity.HoaDon;
import java.math.BigDecimal;

/** Reads the historical discount from stored invoice totals on the original local schema. */
final class HoaDonAmounts {
    private HoaDonAmounts() {}

    static BigDecimal discount(HoaDon invoice) {
        if (invoice.getTienGiamGia() != null) return invoice.getTienGiamGia();
        if (invoice.getTongTien() == null || invoice.getThanhTien() == null) return BigDecimal.ZERO;
        BigDecimal shipping = invoice.getPhiVanChuyen() == null ? BigDecimal.ZERO : invoice.getPhiVanChuyen();
        return invoice.getTongTien().add(shipping).subtract(invoice.getThanhTien()).max(BigDecimal.ZERO);
    }
}
