package com.smashstep.datn.schedule.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** Legacy refunds record their actual actor in a server-generated description. */
public final class TienCaRules {
    private TienCaRules() {}
    private static final Pattern LEGACY_REFUND = Pattern.compile("^Nhân viên ([0-9]+) xác nhận đã hoàn thực tế;.*", Pattern.DOTALL);

    public record Payment(Long owner, Long actor, String allocation, String method,
                          int status, BigDecimal amount, String description) {}

    public static TienCaService.Tien total(Long employee, Long shift, List<Payment> rows) {
        BigDecimal cash = BigDecimal.ZERO, bank = BigDecimal.ZERO, other = BigDecimal.ZERO;
        for (var row : rows) {
            Long actor = row.owner();
            if (ShiftPaymentAttribution.technical(row.allocation())) {
                var parts = row.allocation().substring(ShiftPaymentAttribution.PREFIX.length()).split("\\|", -1);
                if (parts.length != 3 || !parts[2].isEmpty()) throw new IllegalStateException("Invalid payment allocation audit");
                if (!Objects.equals(Long.valueOf(parts[1]), shift)) continue;
                actor = row.actor();
            } else if (row.status() == 2 && row.description() != null) {
                var match = LEGACY_REFUND.matcher(row.description());
                if (match.matches()) actor = Long.valueOf(match.group(1));
            }
            if (!Objects.equals(actor, employee)) continue;
            var amount = row.status() == 2 ? row.amount().abs().negate() : row.amount();
            if ("TIEN_MAT".equals(row.method())) cash = cash.add(amount);
            else if ("CHUYEN_KHOAN".equals(row.method())) bank = bank.add(amount);
            else other = other.add(amount);
        }
        return new TienCaService.Tien(cash, bank, other);
    }
}
