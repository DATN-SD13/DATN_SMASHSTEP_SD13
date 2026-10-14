package com.smashstep.datn.invoice.rules;

import com.smashstep.datn.invoice.entity.HoaDon;

/** Explicit pickup metadata in the existing note column; never inferred from customer selection. */
public final class PosDraftRules {
    public static final String PREFIX = "[POS-DAY1:PICKUP]\n";
    private PosDraftRules() {}
    public static boolean managed(HoaDon h) {
        return h != null && Integer.valueOf(0).equals(h.getLoaiHoaDon())
                && h.getGhiChu() != null && h.getGhiChu().startsWith(PREFIX);
    }
    public static String note(HoaDon h) {
        return managed(h) ? h.getGhiChu().substring(PREFIX.length()) : h.getGhiChu();
    }
}
