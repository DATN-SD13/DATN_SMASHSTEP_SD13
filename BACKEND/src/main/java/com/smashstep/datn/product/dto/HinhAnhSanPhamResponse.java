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
}
