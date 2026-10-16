package com.smashstep.datn.storefront.dto;
import java.util.List;

public record StorefrontFilterResponse(List<Option> brands, List<Option> categories,
        List<Option> colors, List<Option> sizes, List<Option> styles) {
    public record Option(Long id, String name, long productCount, String image) {}
}
