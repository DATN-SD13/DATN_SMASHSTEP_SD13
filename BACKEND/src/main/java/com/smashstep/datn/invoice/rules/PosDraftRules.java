package com.smashstep.datn.invoice.rules;

import com.smashstep.datn.invoice.entity.HoaDon;

/** Explicit pickup metadata in the existing note column; never inferred from customer selection. */
public final class PosDraftRules {
    public static final String PREFIX = "[POS-DAY1:PICKUP]\n";
    public static final String DELIVERY_PREFIX = "[POS-V2:DELIVERY]\n";
    public static final String TON_KHO_THUC = "[TK1]\n";
    private PosDraftRules() {}
    public static boolean daTruKho(HoaDon h) {
        return managed(h) && h.getGhiChu().startsWith((delivery(h) ? DELIVERY_PREFIX : PREFIX) + TON_KHO_THUC);
    }
    public static void danhDauTruKho(HoaDon h) {
        if (!daTruKho(h)) h.setGhiChu((delivery(h) ? DELIVERY_PREFIX : PREFIX) + TON_KHO_THUC + note(h));
    }
    public static boolean managed(HoaDon h) {
        return h != null && Integer.valueOf(0).equals(h.getLoaiHoaDon())
                && h.getGhiChu() != null && (h.getGhiChu().startsWith(PREFIX) || h.getGhiChu().startsWith(DELIVERY_PREFIX));
    }
    public static String note(HoaDon h) {
        if (!managed(h)) return h.getGhiChu();
        int offset = delivery(h) ? DELIVERY_PREFIX.length() : PREFIX.length();
        return h.getGhiChu().substring(offset + (daTruKho(h) ? TON_KHO_THUC.length() : 0));
    }
    public static boolean delivery(HoaDon h) { return managed(h) && h.getGhiChu().startsWith(DELIVERY_PREFIX); }
}
