package com.smashstep.datn.invoice.enums;

import com.smashstep.datn.common.exception.AppException;

public enum TrangThaiHoaDon {
    CHO_XAC_NHAN(0, "Chờ xác nhận", "waiting"),
    DA_XAC_NHAN(1, "Đã xác nhận", "confirmed"),
    CHO_GIAO_HANG(2, "Chờ giao hàng", "ready"),
    DANG_GIAO_HANG(3, "Đang giao hàng", "shipping"),
    DA_GIAO_HANG(4, "Đã giao hàng", "delivered"),
    HOAN_THANH(5, "Hoàn thành", "done"),
    DA_HUY(6, "Đã hủy", "cancel"),
    HOAN_TIEN(7, "Hoàn tiền", "refund"),
    HOA_DON_CHO(8, "Hóa đơn chờ", "pending");

    private final int ma;
    private final String nhan;
    private final String lop;

    TrangThaiHoaDon(int ma, String nhan, String lop) {
        this.ma = ma;
        this.nhan = nhan;
        this.lop = lop;
    }

    public int getMa() { return ma; }
    public String getNhan() { return nhan; }
    public String getLop() { return lop; }

    public static TrangThaiHoaDon trangThaiTiepTheo(Integer maHienTai) {
        if (maHienTai == null || maHienTai < CHO_XAC_NHAN.ma || maHienTai >= HOAN_THANH.ma) {
            return null;
        }
        return tuMa(maHienTai + 1);
    }

    public static TrangThaiHoaDon tuMa(Integer ma) {
        if (ma == null) return null;
        for (TrangThaiHoaDon t : values()) {
            if (t.ma == ma) return t;
        }
        return null;
    }

    public static TrangThaiHoaDon phanTich(String giaTri) {
        if (giaTri == null || giaTri.isBlank() || "all".equalsIgnoreCase(giaTri.trim())) return null;
        String s = giaTri.trim();
        if (s.chars().allMatch(Character::isDigit)) {
            TrangThaiHoaDon t = s.length() > 3 ? null : tuMa(Integer.parseInt(s));
            if (t != null) return t;
        } else {
            for (TrangThaiHoaDon t : values()) {
                if (t.lop.equalsIgnoreCase(s) || t.name().equalsIgnoreCase(s)) return t;
            }
        }
        throw AppException.badRequest("Trạng thái không hợp lệ: " + giaTri);
    }
}