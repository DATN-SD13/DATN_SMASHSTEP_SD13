package com.smashstep.datn.invoice.rules;

import com.smashstep.datn.invoice.dto.HoaDonDetailDto;
import com.smashstep.datn.invoice.entity.LichSuHoaDon;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/** Small immutable audit records in the existing NVARCHAR(1000) history column. */
public final class PosPriceSnapshot {
    private PosPriceSnapshot() {}

    public static final String PREFIX = "[POS-PRICE:1]|";
    public static final String ONLINE_PREFIX = "[ONLINE-PRICE:1]|";

    public static boolean metadata(String note) {
        return note != null && (note.startsWith(PREFIX) || note.startsWith(ONLINE_PREFIX));
    }

    public static String line(
            Long variant, BigDecimal original, BigDecimal sold, BigDecimal percent) {
        return PREFIX
                + "L|"
                + variant
                + "|"
                + original.toPlainString()
                + "|"
                + sold.toPlainString()
                + "|"
                + percent.toPlainString();
    }

    /** Online prices are captured at checkout (COD/QR) or from the verified VNPAY receipt. */
    public static String onlineLine(Long variant, BigDecimal original, BigDecimal sold, BigDecimal percent) {
        // Old receipts may lack original prices: never invent a past campaign from today's catalogue.
        if (variant == null || variant <= 0 || original == null || sold == null
                || original.signum() < 0 || sold.signum() < 0 || original.compareTo(sold) < 0) return null;
        if (percent == null) percent = original.signum() == 0 ? BigDecimal.ZERO
                : original.subtract(sold).multiply(new BigDecimal("100"))
                .divide(original, 2, java.math.RoundingMode.HALF_UP);
        if (percent.signum() < 0 || percent.compareTo(new BigDecimal("100")) > 0) return null;
        return ONLINE_PREFIX + "L|" + variant + "|" + original.toPlainString()
                + "|" + sold.toPlainString() + "|" + percent.toPlainString();
    }

    public static String voucher(String code, Integer type, BigDecimal value) {
        return PREFIX
                + "V|"
                + Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(code.getBytes(StandardCharsets.UTF_8))
                + "|"
                + type
                + "|"
                + value.toPlainString();
    }

    public static void apply(HoaDonDetailDto dto, List<LichSuHoaDon> history) {
        boolean pos = Boolean.TRUE.equals(dto.getPosManaged()) && dto.getNgayThanhToan() != null;
        boolean online = Integer.valueOf(1).equals(dto.getMaLoaiHoaDon());
        if (!pos && !online) return;
        for (var entry : history) {
            String note = entry.getGhiChu();
            if (!metadata(note)) continue;
            boolean onlineRecord = note.startsWith(ONLINE_PREFIX);
            if (onlineRecord ? !online : !pos) continue;
            String prefix = onlineRecord ? ONLINE_PREFIX : PREFIX;
            try {
                String[] p = note.substring(prefix.length()).split("\\|", -1);
                if (p.length == 5 && p[0].equals("L")) {
                    long id = Long.parseLong(p[1]);
                    var original = new BigDecimal(p[2]);
                    var sold = new BigDecimal(p[3]);
                    var percent = new BigDecimal(p[4]);
                    if (original.signum() < 0
                            || sold.signum() < 0
                            || original.compareTo(sold) < 0
                            || percent.signum() < 0
                            || percent.compareTo(new BigDecimal("100")) > 0) continue;
                    for (var item : dto.getChiTietHoaDon())
                        if (Long.valueOf(id).equals(item.getIdSanPhamChiTiet())
                                && sold.compareTo(item.getDonGia()) == 0)
                            item.priceSnapshot(original, percent);
                } else if (!onlineRecord && p.length == 4 && p[0].equals("V")) {
                    String code =
                            new String(Base64.getUrlDecoder().decode(p[1]), StandardCharsets.UTF_8);
                    int type = Integer.parseInt(p[2]);
                    var value = new BigDecimal(p[3]);
                    if ((type == 1 || type == 2) && value.signum() >= 0 && !code.isBlank())
                        dto.voucherSnapshot(code, type, value);
                }
            } catch (RuntimeException invalidRecord) {
                /* Unknown or corrupt metadata must never invent a historical price. */
            }
        }
    }
}
