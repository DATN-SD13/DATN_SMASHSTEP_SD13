package com.smashstep.datn.storefront.dto;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class StorefrontQuery {
    private int page = 0;
    private int size = 12;
    private String keyword = "";
    private String sort = "newest";
    private List<Long> brandId, categoryId, colorId, sizeId, styleId;
    private BigDecimal minPrice, maxPrice;
}
