package com.smashstep.datn.storefront.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Public catalogue data only. Price and discount describe the same lowest-priced eligible variant. */
public record StorefrontProductResponse(
        Long id, String maSanPham, String tenSanPham, Long brandId, String tenThuongHieu,
        Long categoryId, String tenDanhMuc, String tenChatLieu, String tenKieuDang,
        String tenCoGiay, String tenXuatXu, String moTaChiTiet, String anhChinh,
        BigDecimal giaGoc, BigDecimal giaSauGiam, BigDecimal phanTramGiam,
        boolean dangGiamGia, long soLuong, int soBienThe, LocalDateTime ngayTao,
        BigDecimal phanTramGiamCaoNhat, double averageRating, long reviewCount) {
    public StorefrontProductResponse(Long id, String maSanPham, String tenSanPham, Long brandId, String tenThuongHieu,
            Long categoryId, String tenDanhMuc, String tenChatLieu, String tenKieuDang, String tenCoGiay,
            String tenXuatXu, String moTaChiTiet, String anhChinh, BigDecimal giaGoc, BigDecimal giaSauGiam,
            BigDecimal phanTramGiam, boolean dangGiamGia, long soLuong, int soBienThe, LocalDateTime ngayTao,
            BigDecimal phanTramGiamCaoNhat) {
        this(id, maSanPham, tenSanPham, brandId, tenThuongHieu, categoryId, tenDanhMuc, tenChatLieu, tenKieuDang,
                tenCoGiay, tenXuatXu, moTaChiTiet, anhChinh, giaGoc, giaSauGiam, phanTramGiam, dangGiamGia,
                soLuong, soBienThe, ngayTao, phanTramGiamCaoNhat, 0, 0);
    }
}
