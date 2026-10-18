package com.smashstep.datn.storefront.dto;
import java.math.BigDecimal;
import java.util.List;

public record StorefrontVariantResponse(Long id, String maChiTietSanPham, String sku,
        Long mauSacId, String tenMauSac, String maMauHex, Long kichThuocId, String tenKichThuoc,
        int soLuong, BigDecimal giaGoc, BigDecimal giaSauGiam, BigDecimal phanTramGiam,
        boolean dangGiamGia, String anhChinh, List<String> images) {}
