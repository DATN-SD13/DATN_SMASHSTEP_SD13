package com.smashstep.datn.common;

import com.smashstep.datn.employee.controller.NhanVienController;
import com.smashstep.datn.employee.dto.NhanVienRequest;
import com.smashstep.datn.invoice.dto.ThongKeHoaDonDto;
import com.smashstep.datn.sales.dto.SalesResponse.CatalogItem;
import com.smashstep.datn.sales.dto.SalesResponse.PaymentMethod;
import com.smashstep.datn.sales.dto.SalesResponse.Quote;
import com.smashstep.datn.sales.dto.SalesResponse.Receipt;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LegacyJavaDtoCompatibilityTest {
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void statisticsJsonKeepsTheFrontendContract() throws Exception {
        ThongKeHoaDonDto.TrangThai status = new ThongKeHoaDonDto.TrangThai(5, "Hoàn thành", "HOAN_THANH", 3);
        ThongKeHoaDonDto.LoaiDon type = new ThongKeHoaDonDto.LoaiDon(0, "Tại quầy", "TAI_QUAY", 3);
        ThongKeHoaDonDto dto = new ThongKeHoaDonDto(5, 3, 1, 1, 3,
                new BigDecimal("150000.00"), List.of(status), List.of(type));
        JsonNode json = assertJsonKeys(dto, "tongHoaDon", "hoaDonHoanThanh", "hoaDonDaHuy",
                "hoaDonDangXuLy", "hoaDonDaThanhToan", "doanhThuDaThanhToan", "theoTrangThai", "theoLoaiDon");
        assertEquals(5, json.get("tongHoaDon").asLong());
        assertEquals(0, new BigDecimal("150000.00").compareTo(json.get("doanhThuDaThanhToan").decimalValue()));
        assertJsonKeys(status, "ma", "ten", "khoa", "soLuong");
        assertJsonKeys(type, "ma", "ten", "khoa", "soLuong");
        assertImmutableAccessors(dto);
        assertImmutableAccessors(status);
        assertImmutableAccessors(type);
    }

    @Test
    void salesJsonKeepsCatalogPaymentQuoteAndReceiptProperties() {
        assertJsonKeys(catalog(), "id", "code", "productCode", "name", "color", "size", "price", "originalPrice", "stock");
        assertJsonKeys(new PaymentMethod(1L, "TIEN_MAT", "Tiền mặt"), "id", "code", "name");
        Quote quote = new Quote(new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("90.00"));
        JsonNode json = assertJsonKeys(quote, "subtotal", "discount", "total");
        assertEquals(0, quote.total().compareTo(json.get("total").decimalValue()));
        assertJsonKeys(new Receipt(1L, "POS-test", quote.subtotal(), quote.discount(), quote.total(), BigDecimal.ZERO),
                "invoiceId", "invoiceCode", "subtotal", "discount", "total", "change");
    }

    @Test
    void convertedSalesValuesKeepImmutableFieldsAndBothAccessorStyles() throws Exception {
        assertImmutableAccessors(catalog());
        assertImmutableAccessors(new PaymentMethod(1L, null, null));
        assertImmutableAccessors(new Quote(BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE));
        assertImmutableAccessors(new Receipt(1L, "POS-test", BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, null));
    }

    @Test
    void convertedSalesValuesKeepValueEqualityAndReadableText() {
        assertEquals(catalog(), catalog());
        assertEquals(catalog().hashCode(), catalog().hashCode());
        assertEquals(new PaymentMethod(1L, null, null), new PaymentMethod(1L, null, null));
        assertNotEquals(new PaymentMethod(1L, null, null), new PaymentMethod(2L, null, null));
        Quote first = new Quote(BigDecimal.TEN, BigDecimal.ONE, new BigDecimal("9"));
        Quote second = new Quote(BigDecimal.TEN, BigDecimal.ONE, new BigDecimal("9"));
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals("Quote[subtotal=10, discount=1, total=9]", first.toString());
        assertEquals(new Receipt(1L, "POS-test", BigDecimal.TEN, BigDecimal.ONE, new BigDecimal("9"), null),
                new Receipt(1L, "POS-test", BigDecimal.TEN, BigDecimal.ONE, new BigDecimal("9"), null));
    }

    @Test
    void employeeRequestAndControllerDependenciesReallyLoad() throws Exception {
        NhanVienRequest request = new NhanVienRequest();
        request.setName("Nhân viên kiểm tra");
        assertEquals("Nhân viên kiểm tra", request.getName());
        assertSame(NhanVienRequest.class, Class.forName("com.smashstep.datn.employee.dto.NhanVienRequest"));
        assertTrue(NhanVienController.class.getDeclaredMethods().length > 0);
    }

    private CatalogItem catalog() {
        return new CatalogItem(1L, "CT001", "SP001", "Giày", "Đen", "40",
                new BigDecimal("90.00"), new BigDecimal("100.00"), 5);
    }

    private JsonNode assertJsonKeys(Object value, String... keys) {
        JsonNode json = mapper.valueToTree(value);
        assertEquals(keys.length, json.size());
        for (String key : keys) assertTrue(json.has(key), key);
        return json;
    }

    private void assertImmutableAccessors(Object value) throws Exception {
        Class<?> type = value.getClass();
        assertFalse(type.isRecord());
        assertTrue(Modifier.isFinal(type.getModifiers()));
        for (Field field : type.getDeclaredFields()) {
            assertTrue(Modifier.isPrivate(field.getModifiers()));
            assertTrue(Modifier.isFinal(field.getModifiers()));
            field.setAccessible(true);
            String name = field.getName();
            String getter = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
            assertEquals(field.get(value), type.getMethod(name).invoke(value));
            assertEquals(field.get(value), type.getMethod(getter).invoke(value));
        }
    }
}
