package com.smashstep.datn.storefront.dto;
import java.util.List;

public record StorefrontProductDetailResponse(StorefrontProductResponse product,
        List<StorefrontVariantResponse> variants, List<Image> images) {
    public record Image(Long id, Long productId, String urlAnh, boolean isAnhChinh, Long variantId) {}
}
