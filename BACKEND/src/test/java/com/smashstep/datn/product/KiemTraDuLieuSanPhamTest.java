package com.smashstep.datn.product;

import com.smashstep.datn.product.dto.DuLieuSanPham.*;
import com.smashstep.datn.product.dto.*;
import com.smashstep.datn.product.service.QuyTacSanPham;
import jakarta.validation.Validation;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class KiemTraDuLieuSanPhamTest {
    @Test
    void rejectsNegativeInventoryAndZeroPrice() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var invalid = new SanPhamChiTietThemRequest(1L, "SP-1", "SKU-1", 1L, 1L, -1, BigDecimal.ZERO, true, 1);
            var fields = factory.getValidator().validate(invalid).stream().map(v -> v.getPropertyPath().toString()).toList();
            assertTrue(fields.containsAll(List.of("giaBan", "soLuong")));
        }
    }

    @Test
    void rejectsBlankProductAndMissingAttributes() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var invalid = new SanPhamThemRequest(" ", " ", null, null, null, null, null, null, "", 3);
            var fields = factory.getValidator().validate(invalid).stream().map(v -> v.getPropertyPath().toString()).toList();
            assertTrue(fields.containsAll(List.of("maSanPham", "tenSanPham", "danhMucId", "trangThai")));
        }
    }

    @Test
    void acceptsValidVariantAndValidatesNestedBatch() throws Exception {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var valid = new SanPhamChiTietThemRequest(1L, "SP-1", "SKU-1", 1L, 1L, 0, new BigDecimal("1200000.50"), true, 1);
            assertTrue(factory.getValidator().validate(valid).isEmpty());
            assertFalse(factory.getValidator().validate(JsonMapper.builder().build().readValue("{\"variants\":[]}", DanhSachBienTheRequest.class)).isEmpty());
        }
    }

    @Test
    void rejectsFractionalAndFormattedQuantityInEveryVariantBody() {
        var mapper = JsonMapper.builder().build();
        for (String quantity : List.of("1.2", "1.0", "\"10.000\"", "\"abc\"", "2147483648")) {
            String body = "{\"soLuong\":" + quantity + "}";
            assertThrows(Exception.class, () -> mapper.readValue(body, SanPhamChiTietThemRequest.class));
            assertThrows(Exception.class, () -> mapper.readValue(body, SanPhamChiTietSuaRequest.class));
            assertThrows(Exception.class, () -> mapper.readValue(body, DanhSachBienTheRequest.class));
            assertThrows(Exception.class, () -> mapper.readValue("{\"variants\":[" + body + "]}", DanhSachBienTheRequest.class));
        }
    }

    @Test
    void acceptsCleanNumericQuantityWithoutRounding() throws Exception {
        var mapper = JsonMapper.builder().build();
        assertEquals(10000, mapper.readValue("{\"soLuong\":10000}", SanPhamChiTietThemRequest.class).getSoLuong());
        assertEquals(0, mapper.readValue("{\"soLuong\":0}", SanPhamChiTietSuaRequest.class).getSoLuong());
    }

    @Test
    void boundsPaginationAndEscapesSearchWildcards() {
        assertThrows(RuntimeException.class, () -> QuyTacSanPham.taoPhanTrang(-1, 10));
        assertThrows(RuntimeException.class, () -> QuyTacSanPham.taoPhanTrang(0, 101));
        assertEquals("%sp\\%\\_\\[%", QuyTacSanPham.taoMauTimKiem(" SP%_[ "));
    }
    @Test
    void rejectsCoercionOfStatusAndForeignKeys() {
        var mapper = JsonMapper.builder().build();
        for (String body : List.of("{\"trangThai\":1.2}", "{\"danhMucId\":1.2}", "{\"danhMucId\":true}"))
            assertThrows(Exception.class, () -> mapper.readValue(body, SanPhamThemRequest.class));
        assertThrows(Exception.class, () -> mapper.readValue("{\"trangThai\":0.5}", TrangThaiRequest.class));
        assertThrows(Exception.class, () -> mapper.readValue("{\"trangThai\":0.5}", SanPhamTrangThaiRequest.class));
        assertThrows(Exception.class, () -> mapper.readValue("{\"trangThai\":0.5}", SanPhamChiTietTrangThaiRequest.class));
        assertThrows(Exception.class, () -> mapper.readValue("{\"mauSacId\":1.2}", SanPhamChiTietThemRequest.class));
        assertThrows(Exception.class, () -> mapper.readValue("{\"kichThuocId\":true}", SanPhamChiTietSuaRequest.class));
        assertThrows(Exception.class, () -> mapper.readValue("{\"trangThai\":1.2}", ThuocTinhRequest.class));
    }

    @Test
    void acceptsNumericAndCanonicalStringPricesWithoutLosingPrecision() throws Exception {
        var mapper = JsonMapper.builder().build();
        for (String price : List.of("1", "1250000.50", "9007199254740991.99", "9999999999999999.99")) {
            for (String token : List.of(price, "\"" + price + "\"")) {
                String body = "{\"giaBan\":" + token + "}";
                assertEquals(new BigDecimal(price), mapper.readValue(body, SanPhamChiTietThemRequest.class).getGiaBan());
                assertEquals(new BigDecimal(price), mapper.readValue(body, SanPhamChiTietSuaRequest.class).getGiaBan());
                var batch = mapper.readValue("{\"variants\":[" + body + "]}", DanhSachBienTheRequest.class);
                assertEquals(new BigDecimal(price), batch.getVariants().get(0).getGiaBan());
            }
        }
    }

    @Test
    void createAndEditSharePositivePriceAndDecimalBounds() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var create = new SanPhamChiTietThemRequest(1L, "SP-PRICE", "SKU-PRICE", 1L, 1L, 0, null, true, 1);
            var edit = new SanPhamChiTietSuaRequest(1L, "SP-PRICE", "SKU-PRICE", 1L, 1L, 0, null, true, 1);
            for (String price : List.of("1", "0.01", "1250000.50", "9007199254740991.99", "9999999999999999.99")) {
                create.setGiaBan(new BigDecimal(price)); edit.setGiaBan(new BigDecimal(price));
                assertTrue(factory.getValidator().validate(create).isEmpty(), price);
                assertTrue(factory.getValidator().validate(edit).isEmpty(), price);
            }
            for (BigDecimal price : java.util.Arrays.asList(null, BigDecimal.ZERO, new BigDecimal("-1"), new BigDecimal("10000000000000000"), new BigDecimal("1250000.123"))) {
                create.setGiaBan(price); edit.setGiaBan(price);
                assertTrue(factory.getValidator().validate(create).stream().anyMatch(v -> v.getPropertyPath().toString().equals("giaBan")));
                assertTrue(factory.getValidator().validate(edit).stream().anyMatch(v -> v.getPropertyPath().toString().equals("giaBan")));
            }
        }
    }

}
