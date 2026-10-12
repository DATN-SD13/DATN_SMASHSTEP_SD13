package com.smashstep.datn.product;

import com.smashstep.datn.DatnApplication;
import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.product.dto.*;
import com.smashstep.datn.product.dto.DuLieuSanPham.*;
import com.smashstep.datn.product.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = DatnApplication.class, properties = {
        "spring.jpa.open-in-view=false", "spring.jpa.hibernate.ddl-auto=none", "spring.jpa.show-sql=false" })
@Transactional
class ProductDuplicateTichHopTest {
    @Autowired SanPhamService products;
    @Autowired ThuocTinhSanPhamService attributes;
    @Autowired EntityManager entityManager;
    @Autowired WebApplicationContext context;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final Map<String, Long> ids = new LinkedHashMap<>();
    private final String key = "DUP-" + UUID.randomUUID().toString().substring(0, 12);
    private MockMvc mvc;

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        for (String type : List.of("categories", "brands", "materials", "styles", "collars", "origins")) {
            ids.put(type, attributes.luuThuocTinh(type, null,
                    new ThuocTinhRequest(null, key + type, null, null, 1)).getId());
        }
    }

    private SanPhamThemRequest request(String suffix) {
        return new SanPhamThemRequest(key + suffix, "Giày Đẹp " + key,
                ids.get("categories"), ids.get("brands"), ids.get("materials"), ids.get("styles"),
                ids.get("collars"), ids.get("origins"), "", 1);
    }
    private String json(Object value) throws Exception { return mapper.writeValueAsString(value); }
    private SanPhamSuaRequest update(SanPhamThemRequest r) {
        return new SanPhamSuaRequest(null, r.getTenSanPham(), r.getDanhMucId(), r.getThuongHieuId(),
                r.getChatLieuId(), r.getKieuDangId(), r.getCoGiayId(), r.getXuatXuId(), "changed", r.getTrangThai());
    }
    private String hex() { return String.format("#%06X", UUID.randomUUID().hashCode() & 0xFFFFFF); }

    @Test void normalizationKeepsVietnameseAccentsAndSizesAreStrings() {
        assertEquals("giày đẹp", QuyTacSanPham.normalizeBusinessName("  GIÀY\t  Đẹp "));
        assertNotEquals(QuyTacSanPham.normalizeBusinessName("giày"), QuyTacSanPham.normalizeBusinessName("giay"));
        assertNotEquals(QuyTacSanPham.normalizeBusinessName("40"), QuyTacSanPham.normalizeBusinessName("40.0"));
    }

    @Test void exactCaseWhitespaceAndInactiveAreDuplicatesWithFullSummaryAnd409() throws Exception {
        var r = request("1"); r.setTenSanPham("Giày   Đẹp " + key); r.setTrangThai(0);
        var existing = products.themSanPham(r);
        r.setMaSanPham(key + "2"); r.setTenSanPham("  giày  đẹp   " + key.toLowerCase(Locale.ROOT) + "  ");
        var check = SanPhamTrungRequest.from(r, null);
        mvc.perform(post("/api/products/check-duplicate").contentType(MediaType.APPLICATION_JSON).content(json(check)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.duplicate").value(true))
                .andExpect(jsonPath("$.product.id").value(existing.getId()))
                .andExpect(jsonPath("$.product.maSanPham").value(existing.getMaSanPham()))
                .andExpect(jsonPath("$.product.tenDanhMuc").isNotEmpty())
                .andExpect(jsonPath("$.product.tenThuongHieu").isNotEmpty())
                .andExpect(jsonPath("$.product.tenChatLieu").isNotEmpty())
                .andExpect(jsonPath("$.product.tenKieuDang").isNotEmpty())
                .andExpect(jsonPath("$.product.tenCoGiay").isNotEmpty())
                .andExpect(jsonPath("$.product.tenXuatXu").isNotEmpty())
                .andExpect(jsonPath("$.product.trangThai").value(0));
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(json(r)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Sản phẩm với tên và bộ thuộc tính này đã tồn tại."));
    }

    @ParameterizedTest @ValueSource(strings = {"categories", "brands", "materials", "styles", "collars", "origins"})
    void sameNameDifferentAnyAttributeCanCreate(String type) {
        products.themSanPham(request("1"));
        Long other = attributes.luuThuocTinh(type, null, new ThuocTinhRequest(null, key + "other", null, null, 1)).getId();
        var r = request("2");
        switch (type) {
            case "categories" -> r.setDanhMucId(other);
            case "brands" -> r.setThuongHieuId(other);
            case "materials" -> r.setChatLieuId(other);
            case "styles" -> r.setKieuDangId(other);
            case "collars" -> r.setCoGiayId(other);
            case "origins" -> r.setXuatXuId(other);
        }
        assertFalse(products.kiemTraTrung(SanPhamTrungRequest.from(r, null)).isDuplicate());
        assertNotNull(products.themSanPham(r).getId());
    }

    @Test void differentNameSameAttributesCanCreateAndAccentlessNameIsDifferent() {
        products.themSanPham(request("1"));
        var r = request("2"); r.setTenSanPham("Giay Dep " + key);
        assertFalse(products.kiemTraTrung(SanPhamTrungRequest.from(r, null)).isDuplicate());
        assertNotNull(products.themSanPham(r).getId());
    }

    @Test void updateExcludesSelfButRejectsOtherIdentityWithoutOverwriting() throws Exception {
        var first = products.themSanPham(request("1"));
        var self = SanPhamTrungRequest.from(request("1"), first.getId());
        assertFalse(products.kiemTraTrung(self).isDuplicate());
        mvc.perform(put("/api/products/" + first.getId()).contentType(MediaType.APPLICATION_JSON).content(json(update(request("1")))))
                .andExpect(status().isOk());
        var otherRequest = request("2"); otherRequest.setTenSanPham("Other " + key);
        var other = products.themSanPham(otherRequest);
        mvc.perform(put("/api/products/" + other.getId()).contentType(MediaType.APPLICATION_JSON).content(json(update(request("1")))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value(
                        "Đã tồn tại sản phẩm " + first.getMaSanPham() + " có cùng tên và bộ thuộc tính."));
        entityManager.flush(); entityManager.clear();
        assertEquals(otherRequest.getTenSanPham(), products.layChiTietSanPham(other.getId()).getProduct().getTenSanPham());
    }

    @ParameterizedTest @ValueSource(strings = {"brands", "categories", "materials", "styles", "collars", "origins", "colors", "sizes"})
    void allEightAttributesNormalizeNamesIncludeInactiveAndExcludeSelf(String type) throws Exception {
        String color = "colors".equals(type) ? hex() : null;
        String words = "sizes".equals(type) ? "Size Value" : "Giá Trị";
        var r = new ThuocTinhRequest(null, key + "   " + words.replace(" ", "   "), null, color, 0);
        var existing = attributes.luuThuocTinh(type, null, r);
        String preview = "sizes".equals(type) ? null : attributes.taoMaTiepTheo(type);
        String normalized = "  " + (key + " " + words).toLowerCase(Locale.ROOT) + "  ";
        var check = new ThuocTinhTrungRequest(normalized, color, null);
        mvc.perform(post("/api/product-attributes/" + type + "/check-duplicate").contentType(MediaType.APPLICATION_JSON).content(json(check)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.duplicate").value(true))
                .andExpect(jsonPath("$.attribute.id").value(existing.getId()))
                .andExpect(jsonPath("$.attribute.trangThai").value(0));
        if (preview != null) assertEquals(preview, attributes.taoMaTiepTheo(type));
        r.setTen(normalized);
        mvc.perform(post("/api/product-attributes/" + type).contentType(MediaType.APPLICATION_JSON).content(json(r)))
                .andExpect(status().isConflict());
        check.setExcludeId(existing.getId());
        assertFalse(attributes.kiemTraTrung(type, check).isDuplicate());
        mvc.perform(put("/api/product-attributes/" + type + "/" + existing.getId()).contentType(MediaType.APPLICATION_JSON).content(json(r)))
                .andExpect(status().isOk());
        var other = attributes.luuThuocTinh(type, null,
                new ThuocTinhRequest(null, key + "Other", null, color == null ? null : hex(), 1));
        mvc.perform(put("/api/product-attributes/" + type + "/" + other.getId()).contentType(MediaType.APPLICATION_JSON).content(json(r)))
                .andExpect(status().isConflict());
        assertEquals(key + "Other", attributes.layChiTiet(type, other.getId()).getTen());
    }

    @Test void colorHexIsCaseInsensitiveAndReportsNameHexAndBoth() throws Exception {
        String hex = hex();
        var r = new ThuocTinhRequest(null, key + " Red", null, hex.toLowerCase(Locale.ROOT), 1);
        var existing = attributes.luuThuocTinh("colors", null, r);
        assertEquals(hex, existing.getMaMauHex());
        var check = new ThuocTinhTrungRequest(key + "Other", hex, null);
        assertEquals("HEX", attributes.kiemTraTrung("colors", check).getReason());
        check.setTen(r.getTen()); assertEquals("NAME_AND_HEX", attributes.kiemTraTrung("colors", check).getReason());
        check.setMaMauHex(hex()); assertEquals("NAME", attributes.kiemTraTrung("colors", check).getReason());
        r.setTen(key + "Other");
        mvc.perform(post("/api/product-attributes/colors").contentType(MediaType.APPLICATION_JSON).content(json(r)))
                .andExpect(status().isConflict());
    }

    @Test void sizes40And40Point0RemainDifferentStrings() {
        var r = new ThuocTinhRequest(null, key + "40", null, null, 1);
        attributes.luuThuocTinh("sizes", null, r); r.setTen(key + "40.0");
        assertFalse(attributes.kiemTraTrung("sizes", new ThuocTinhTrungRequest(r.getTen(), null, null)).isDuplicate());
        assertNotNull(attributes.luuThuocTinh("sizes", null, r).getId());
    }

    @Test void invalidInputUnknownTypesAndMissingIdsHaveExpectedStatuses() throws Exception {
        mvc.perform(post("/api/products/check-duplicate").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/product-attributes/brands/check-duplicate").contentType(MediaType.APPLICATION_JSON).content("{\"ten\":\" \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/product-attributes/colors/check-duplicate").contentType(MediaType.APPLICATION_JSON).content("{\"ten\":\"Red\",\"maMauHex\":\"red\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/product-attributes/unknown/check-duplicate").contentType(MediaType.APPLICATION_JSON).content("{\"ten\":\"a\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/products/9223372036854775807")).andExpect(status().isNotFound());
        mvc.perform(get("/api/product-attributes/brands/9223372036854775807")).andExpect(status().isNotFound());
    }
}
