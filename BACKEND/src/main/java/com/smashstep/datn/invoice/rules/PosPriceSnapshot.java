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

    public static boolean metadata(String note) {
        return note != null && note.startsWith(PREFIX);
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
        if (!Boolean.TRUE.equals(dto.getPosManaged()) || dto.getNgayThanhToan() == null) return;
        for (var entry : history) {
            String note = entry.getGhiChu();
            if (!metadata(note)) continue;
            try {
                String[] p = note.substring(PREFIX.length()).split("\\|", -1);
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
                } else if (p.length == 4 && p[0].equals("V")) {
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
