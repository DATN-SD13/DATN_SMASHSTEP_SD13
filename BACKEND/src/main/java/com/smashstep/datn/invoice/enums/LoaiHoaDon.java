package com.smashstep.datn.invoice.enums;

import com.smashstep.datn.common.exception.AppException;

public enum LoaiHoaDon {
    TAI_QUAY(0, "Tại quầy", "store"),
    TRUC_TUYEN(1, "Trực tuyến", "online"),
    GIAO_HANG_LEGACY(2, "Giao hàng", "delivery");

    private final int ma;
    private final String nhan;
    private final String khoa;

    LoaiHoaDon(int ma, String nhan, String khoa) {
        this.ma = ma;
        this.nhan = nhan;
        this.khoa = khoa;
    }

    public int getMa() { return ma; }
    public String getNhan() { return nhan; }
    public String getKhoa() { return khoa; }

    public static LoaiHoaDon tuMa(Integer ma) {
        if (ma == null) return null;
        for (LoaiHoaDon l : values()) if (l.ma == ma) return l;
        return null;
    }

    public static LoaiHoaDon phanTich(String giaTri) {
        if (giaTri == null || giaTri.isBlank() || "all".equalsIgnoreCase(giaTri.trim())) return null;
        String s = giaTri.trim();
        if (s.chars().allMatch(Character::isDigit)) {
            LoaiHoaDon l = s.length() > 3 ? null : tuMa(Integer.parseInt(s));
            if (l != null) return l;
        } else {
            for (LoaiHoaDon l : values()) {
                if (l.khoa.equalsIgnoreCase(s) || l.name().equalsIgnoreCase(s)) return l;
            }
        }
        throw AppException.badRequest("Loại hóa đơn không hợp lệ: " + giaTri);
    }
}
