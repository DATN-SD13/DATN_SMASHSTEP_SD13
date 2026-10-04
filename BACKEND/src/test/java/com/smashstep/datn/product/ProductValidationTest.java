package com.smashstep.datn.product;

import com.smashstep.datn.product.dto.ProductDtos.*;
import com.smashstep.datn.product.service.ProductRules;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ProductValidationTest {
    @Test
    void rejectsNegativeInventoryAndZeroPrice() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var invalid = new VariantRequest(1L, "SP-1", "SKU-1", 1L, 1L, -1, BigDecimal.ZERO, true, 1);
            var fields = factory.getValidator().validate(invalid).stream().map(v -> v.getPropertyPath().toString()).toList();
            assertTrue(fields.containsAll(List.of("giaBan", "soLuong")));
        }
    }

    @Test
    void rejectsBlankProductAndMissingAttributes() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var invalid = new ProductRequest(" ", " ", null, null, null, null, null, null, "", 3);
            var fields = factory.getValidator().validate(invalid).stream().map(v -> v.getPropertyPath().toString()).toList();
            assertTrue(fields.containsAll(List.of("maSanPham", "tenSanPham", "danhMucId", "trangThai")));
        }
    }

    @Test
    void acceptsValidVariantAndValidatesNestedBatch() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var valid = new VariantRequest(1L, "SP-1", "SKU-1", 1L, 1L, 0, new BigDecimal("1200000.50"), true, 1);
            assertTrue(factory.getValidator().validate(valid).isEmpty());
            assertFalse(factory.getValidator().validate(new VariantBatchRequest(List.of())).isEmpty());
        }
    }

    @Test
    void boundsPaginationAndEscapesSearchWildcards() {
        assertThrows(RuntimeException.class, () -> ProductRules.page(-1, 10));
        assertThrows(RuntimeException.class, () -> ProductRules.page(0, 101));
        assertEquals("%sp\\%\\_\\[%", ProductRules.like(" SP%_[ "));
    }
}

