package com.smashstep.datn.invoice.rules;

import com.smashstep.datn.invoice.entity.HoaDon;
import com.smashstep.datn.invoice.enums.HinhThucNhan;
import com.smashstep.datn.invoice.enums.LoaiHoaDon;

/** Delivery behavior derived exclusively from canonical invoice data; no database query. */
public final class HoaDonRules {
    private HoaDonRules() {}

    public static HinhThucNhan resolveHinhThucNhan(HoaDon invoice) {
        if (invoice == null) return HinhThucNhan.NHAN_TAI_QUAY;
        Integer type = invoice.getLoaiHoaDon();
        if (Integer.valueOf(LoaiHoaDon.TRUC_TUYEN.getMa()).equals(type)
                || Integer.valueOf(LoaiHoaDon.GIAO_HANG_LEGACY.getMa()).equals(type)) {
            return HinhThucNhan.GIAO_HANG;
        }
        if (Integer.valueOf(LoaiHoaDon.TAI_QUAY.getMa()).equals(type)) {
            return invoice.getIdKhachHang() == null ? HinhThucNhan.NHAN_TAI_QUAY : HinhThucNhan.GIAO_HANG;
        }
        return notBlank(invoice.getDiaChiGiaoHang()) || notBlank(invoice.getDonViVanChuyen())
                ? HinhThucNhan.GIAO_HANG : HinhThucNhan.NHAN_TAI_QUAY;
    }

    private static boolean notBlank(String value) { return value != null && !value.isBlank(); }
}
