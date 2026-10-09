package com.smashstep.datn.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HinhAnhSanPhamResponse {
    private Long id;
    private String urlAnh;
    private Boolean isAnhChinh;
    private Long sanPhamId;
    private Long sanPhamChiTietId;

    public HinhAnhSanPhamResponse(Long id, String urlAnh, Boolean isAnhChinh, Long sanPhamId) {
        this(id, urlAnh, isAnhChinh, sanPhamId, null);
    }

    public HinhAnhSanPhamResponse(Long id, String urlAnh, Boolean isAnhChinh) {
        this(id, urlAnh, isAnhChinh, null);
    }
}
