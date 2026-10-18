package com.smashstep.datn.sales.service;
import com.smashstep.datn.invoice.entity.HoaDon;
import java.time.*;
public final class PosDayRules {
    private PosDayRules() {}
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static LocalDateTime startToday() { return LocalDate.now(ZONE).atStartOfDay(); }
    public static boolean expired(HoaDon h, LocalDateTime start) {
        return h.getNgayTao() != null && h.getNgayTao().isBefore(start);
    }
}
