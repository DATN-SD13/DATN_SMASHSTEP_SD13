package com.smashstep.datn.product.dto;

/** Không cho Jackson làm tròn số lượng thập phân hoặc chuyển chuỗi có dấu phân nhóm. */
final class SoLuongSanPham {
    private SoLuongSanPham() {}

    private static java.math.BigInteger soNguyen(Object value, String label) {
        if (value == null) return null;
        if (!(value instanceof Byte || value instanceof Short || value instanceof Integer
                || value instanceof Long || value instanceof java.math.BigInteger))
            throw new IllegalArgumentException(label + " phải là số nguyên, không dùng chuỗi hoặc số thập phân.");
        return new java.math.BigInteger(value.toString());
    }

    static Long docId(Object value) {
        var number = soNguyen(value, "ID thuộc tính/sản phẩm");
        if (number == null) return null;
        try { return number.longValueExact(); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("ID vượt quá giới hạn cho phép.", e); }
    }

    static Integer docTrangThai(Object value) {
        var number = soNguyen(value, "Trạng thái");
        if (number == null) return null;
        try { return number.intValueExact(); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("Trạng thái phải là 0 hoặc 1.", e); }
    }

    static Integer doc(Object value) {
        if (value == null) return null;
        if (!(value instanceof Byte || value instanceof Short || value instanceof Integer
                || value instanceof Long || value instanceof java.math.BigInteger))
            throw new IllegalArgumentException("Số lượng phải là số nguyên, không dùng chuỗi hoặc số thập phân.");
        try { return new java.math.BigInteger(value.toString()).intValueExact(); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("Số lượng tối đa 2.147.483.647.", e); }
    }
}
