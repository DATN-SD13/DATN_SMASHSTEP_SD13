package com.smashstep.datn.invoice.rules;

import com.smashstep.datn.invoice.entity.HoaDon;
import com.smashstep.datn.invoice.entity.LichSuHoaDon;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Nguồn thao tác do server gắn; không đoán từ câu mô tả người dùng. */
public final class PosHistoryRules {
    private PosHistoryRules() {}

    public static final String KY_THUAT = "[POS-DRAFT:1]|";
    public static final String HOAN_TAT = "[POS-CHECKOUT:1]|";
    public static final String THAO_TAC_POS = "[POS-ACTION:1]|";

    public static boolean danhRieng(String note) {
        return note != null
                && (note.startsWith(KY_THUAT)
                || note.startsWith(HOAN_TAT)
                || note.startsWith(THAO_TAC_POS)
                || note.startsWith(com.smashstep.datn.storefront.checkout.OnlineOrderInventory.PREFIX)
                || note.startsWith(com.smashstep.datn.storefront.checkout.VnpayOrderService.PREFIX)
                || com.smashstep.datn.storefront.checkout.OrderEmailNotifications.technical(note)
                || note.startsWith(com.smashstep.datn.storefront.checkout.OrderLocation.PREFIX));
    }

    public static boolean hienThi(HoaDon invoice, LichSuHoaDon row) {
        if (row.getGhiChu() != null && (row.getGhiChu().startsWith(com.smashstep.datn.storefront.checkout.OnlineOrderInventory.PREFIX)
                || row.getGhiChu().startsWith(com.smashstep.datn.storefront.checkout.VnpayOrderService.PREFIX)
                || com.smashstep.datn.storefront.checkout.OrderEmailNotifications.technical(row.getGhiChu())
                || row.getGhiChu().startsWith(com.smashstep.datn.storefront.checkout.OrderLocation.PREFIX))) return false;
        String note = row.getGhiChu();
        if (PosPriceSnapshot.metadata(note)) return false;
        if (!PosDraftRules.managed(invoice)) return true;
        if (note != null && (note.startsWith(KY_THUAT) || note.startsWith(THAO_TAC_POS)))
            return false;
        // Cả giỏ cũ trước bản cập nhật: trạng thái 8 là mốc giỏ chờ, không phải thao tác quản lý
        // đơn đã bán.
        return !Integer.valueOf(8).equals(row.getTrangThai());
    }

    public static String noiDung(String note) {
        return note != null && note.startsWith(HOAN_TAT) ? note.substring(HOAN_TAT.length()) : note;
    }

    /** Group repeated POS confirmation attempts without deleting the original audit rows.
     * Keep the latest attempt in each consecutive group, including its actual actor and time.
     */
    public static List<LichSuHoaDon> hienThiRutGon(HoaDon invoice, List<LichSuHoaDon> rows) {
        var sorted = new ArrayList<>(rows);
        sorted.sort(Comparator.comparing(LichSuHoaDon::getNgayTao,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(LichSuHoaDon::getId,
                        Comparator.nullsLast(Comparator.reverseOrder())));
        var result = new ArrayList<LichSuHoaDon>();
        boolean previousWasConfirmation = false;
        for (var row : sorted) {
            if (!hienThi(invoice, row)) continue;
            boolean confirmation = PosDraftRules.managed(invoice)
                    && Integer.valueOf(0).equals(row.getTrangThai())
                    && (HOAN_TAT + "Chờ xác nhận thanh toán tại POS").equals(row.getGhiChu());
            if (!confirmation || !previousWasConfirmation) result.add(row);
            previousWasConfirmation = confirmation;
        }
        return result;
    }
}
