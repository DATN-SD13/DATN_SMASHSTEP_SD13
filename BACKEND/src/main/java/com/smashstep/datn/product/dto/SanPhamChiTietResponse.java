package com.smashstep.datn.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SanPhamChiTietResponse {
    private SanPhamResponse product;
    private List<BienTheResponse> variants;
    private List<HinhAnhSanPhamResponse> images;
}
