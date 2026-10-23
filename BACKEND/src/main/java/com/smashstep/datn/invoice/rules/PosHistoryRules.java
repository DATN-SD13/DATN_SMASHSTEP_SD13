package com.smashstep.datn.invoice.rules;

import com.smashstep.datn.invoice.entity.HoaDon;
import com.smashstep.datn.invoice.entity.LichSuHoaDon;

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
                || note.startsWith(com.smashstep.datn.storefront.checkout.VnpayOrderService.PREFIX));
    }

    public static boolean hienThi(HoaDon invoice, LichSuHoaDon row) {
        if (row.getGhiChu() != null && (row.getGhiChu().startsWith(com.smashstep.datn.storefront.checkout.OnlineOrderInventory.PREFIX)
                || row.getGhiChu().startsWith(com.smashstep.datn.storefront.checkout.VnpayOrderService.PREFIX))) return false;
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
}
