package com.smashstep.datn.invoice.enums;

import com.smashstep.datn.common.exception.AppException;

public enum HinhThucNhan {
    NHAN_TAI_QUAY(0, "Nhận tại quầy", "pickup"),
    GIAO_HANG(1, "Giao hàng", "delivery");

    private final int ma;
    private final String nhan;
    private final String khoa;

    HinhThucNhan(int ma, String nhan, String khoa) {
        this.ma = ma; this.nhan = nhan; this.khoa = khoa;
    }
    public int getMa() { return ma; }
    public String getNhan() { return nhan; }
    public String getKhoa() { return khoa; }

    public static HinhThucNhan tuMa(Integer ma) {
        if (ma == null) return null;
        for (HinhThucNhan h : values()) if (h.ma == ma) return h;
        return null;
    }
    public static HinhThucNhan phanTich(String giaTri) {
        if (giaTri == null || giaTri.isBlank()) return null;
        String s = giaTri.trim();
        if (s.chars().allMatch(Character::isDigit)) return tuMa(Integer.parseInt(s));
        for (HinhThucNhan h : values()) {
            if (h.khoa.equalsIgnoreCase(s) || h.name().equalsIgnoreCase(s)) return h;
        }
        throw AppException.badRequest("Hình thức nhận không hợp lệ: " + giaTri);
    }
}
