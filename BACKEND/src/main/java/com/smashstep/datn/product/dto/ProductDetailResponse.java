package com.smashstep.datn.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailResponse {
    private ProductResponse product;
    private List<VariantResponse> variants;
    private List<ImageResponse> images;
}
