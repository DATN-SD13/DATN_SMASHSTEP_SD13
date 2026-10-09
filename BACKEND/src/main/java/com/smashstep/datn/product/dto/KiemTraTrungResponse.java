package com.smashstep.datn.product.dto;

import com.smashstep.datn.product.dto.DuLieuSanPham.ThuocTinhResponse;
import lombok.*;

public final class KiemTraTrungResponse {
    private KiemTraTrungResponse() {}

    @Getter @AllArgsConstructor
    public static class Product {
        private final boolean duplicate;
        private final SanPhamResponse product;
    }

    @Getter @AllArgsConstructor
    public static class Attribute {
        private final boolean duplicate;
        private final ThuocTinhResponse attribute;
        private final String reason;
    }
}
