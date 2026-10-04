package com.smashstep.datn.product.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/** This module uses 1 = active, 0 = inactive. No previous numeric convention exists in the repo. */
public final class ProductRules {
    private ProductRules() {}
    public static String trim(String value) { return value == null ? "" : value.trim(); }
    public static void status(Integer status) {
        if (status != null && status != 0 && status != 1) throw ProductException.badRequest("Trạng thái phải là 0 hoặc 1");
    }
    public static PageRequest page(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw ProductException.badRequest("Trang phải từ 0, kích thước trang từ 1 đến 100");
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
    }
    public static String like(String keyword) {
        return "%" + trim(keyword).toLowerCase(java.util.Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_").replace("[", "\\[") + "%";
    }
}

