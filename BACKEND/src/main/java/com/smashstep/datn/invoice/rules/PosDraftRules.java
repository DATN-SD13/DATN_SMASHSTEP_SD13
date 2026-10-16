package com.smashstep.datn.invoice.rules;

import com.smashstep.datn.invoice.entity.HoaDon;

/** Explicit pickup metadata in the existing note column; never inferred from customer selection. */
public final class PosDraftRules {
    public static final String PREFIX = "[POS-DAY1:PICKUP]\n";
    public static final String DELIVERY_PREFIX = "[POS-V2:DELIVERY]\n";
    private PosDraftRules() {}
    public static boolean managed(HoaDon h) {
        return h != null && Integer.valueOf(0).equals(h.getLoaiHoaDon())
                && h.getGhiChu() != null && (h.getGhiChu().startsWith(PREFIX) || h.getGhiChu().startsWith(DELIVERY_PREFIX));
    }
    public static String note(HoaDon h) {
        return managed(h) ? h.getGhiChu().substring(delivery(h) ? DELIVERY_PREFIX.length() : PREFIX.length()) : h.getGhiChu();
    }
    public static boolean delivery(HoaDon h) { return managed(h) && h.getGhiChu().startsWith(DELIVERY_PREFIX); }
}
