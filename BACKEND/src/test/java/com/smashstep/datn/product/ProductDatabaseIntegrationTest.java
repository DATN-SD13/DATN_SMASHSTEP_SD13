package com.smashstep.datn.product;

import com.smashstep.datn.DatnApplication;
import com.smashstep.datn.product.dto.ProductDtos.*;
import com.smashstep.datn.product.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Uses the configured SQL Server. Every test transaction is rolled back; no schema changes or committed seed data. */
@SpringBootTest(classes = DatnApplication.class, properties = {
        "spring.jpa.open-in-view=false", "spring.jpa.hibernate.ddl-auto=none", "spring.jpa.show-sql=false" })
@Transactional
class ProductDatabaseIntegrationTest {
    @Autowired ProductService products;
    @Autowired VariantService variants;
    @Autowired ProductAttributeService attributes;
    @Autowired EntityManager entityManager;
    @Autowired WebApplicationContext context;
    private final Map<String, Long> ids = new LinkedHashMap<>();
    private String code;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        code = "TEST-" + UUID.randomUUID().toString().substring(0, 12);
        for (String type : List.of("categories", "brands", "materials", "styles", "collars", "origins", "colors", "sizes")) {
            var request = new AttributeRequest(code, code + "-" + type, "Ghi chú kiểm thử",
                    type.equals("colors") ? "#123456" : null, 1);
            ids.put(type, attributes.save(type, null, request).id());
        }
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private ProductRequest productRequest() {
        return new ProductRequest(code, "Giày kiểm thử " + code, ids.get("categories"), ids.get("brands"),
                ids.get("materials"), ids.get("styles"), ids.get("collars"), ids.get("origins"), "Mô tả", 1);
    }

    private VariantRequest variantRequest(Long productId, Long sizeId, int quantity, String price) {
        String key = code + "-" + sizeId;
        return new VariantRequest(productId, key, key, ids.get("colors"), sizeId, quantity,
                new BigDecimal(price), true, 1);
    }

    @Test
    void persistsVariantsAndReturnsRealPaginatedAggregates() throws Exception {
        var product = products.create(productRequest());
        var size2 = attributes.save("sizes", null, new AttributeRequest(null, code + "-size2", "", null, 1));
        variants.createBatch(product.id(), List.of(
                variantRequest(product.id(), ids.get("sizes"), 2, "1200000"),
                variantRequest(product.id(), size2.id(), 3, "1500000")));
        entityManager.flush(); entityManager.clear();
        var page = products.list(0, 1, code, 1, ids.get("categories"), ids.get("brands"),
                ids.get("materials"), ids.get("styles"), ids.get("collars"), ids.get("origins"));
        assertEquals(1, page.totalElements()); assertEquals(1, page.content().size());
        var row = page.content().get(0);
        assertEquals(5, row.tongSoLuong()); assertEquals(2, row.tongSoBienThe());
        assertEquals(0, new BigDecimal("1200000").compareTo(row.giaThapNhat()));
        assertEquals(0, new BigDecimal("1500000").compareTo(row.giaCaoNhat()));
        assertTrue(products.list(1, 1, code, null, null, null, null, null, null, null).content().isEmpty());
        assertEquals(2, variants.list(0, 10, code, 1, product.id(), ids.get("colors"), null).totalElements());
        assertEquals(1, variants.list(0, 10, code, 1, product.id(), ids.get("colors"), size2.id()).totalElements());
        mvc.perform(get("/api/products/" + product.id())).andExpect(status().isOk())
                .andExpect(jsonPath("$.product.tongSoLuong").value(5)).andExpect(jsonPath("$.variants.length()").value(2));
    }

    @Test
    void editKeepsVariantsAndImageSelectionMaintainsOneMainImage() {
        var product = products.create(productRequest());
        var variant = variants.create(variantRequest(product.id(), ids.get("sizes"), 4, "1000000"));
        var updated = new ProductRequest(code, "Tên đã sửa", ids.get("categories"), ids.get("brands"),
                ids.get("materials"), ids.get("styles"), ids.get("collars"), ids.get("origins"), "Mô tả đã sửa", 0);
        products.update(product.id(), updated);
        var first = products.addImage(product.id(), new ImageRequest("https://example.com/first.png", false));
        products.addImage(product.id(), new ImageRequest("https://example.com/second.png", true));
        entityManager.flush(); entityManager.clear();
        var detail = products.detail(product.id());
        assertEquals(1, detail.variants().size()); assertEquals(variant.id(), detail.variants().get(0).id());
        assertEquals("Tên đã sửa", detail.product().tenSanPham()); assertNotNull(detail.product().ngayCapNhat());
        assertEquals(1, detail.images().stream().filter(ImageResponse::isAnhChinh).count());
        products.updateImage(product.id(), first.id(), new ImageRequest(first.urlAnh(), true));
        variants.update(variant.id(), new VariantRequest(product.id(), variant.maChiTietSanPham(), variant.sku(),
                variant.mauSacId(), variant.kichThuocId(), 7, new BigDecimal("1100000"), false, 0));
        entityManager.flush(); entityManager.clear();
        var refreshed = products.detail(product.id());
        assertEquals(7, refreshed.product().tongSoLuong());
        assertEquals(1, refreshed.images().stream().filter(ImageResponse::isAnhChinh).count());
        assertTrue(refreshed.images().stream().filter(i -> i.id().equals(first.id())).findFirst().orElseThrow().isAnhChinh());
        assertFalse(refreshed.variants().get(0).kichHoat());
    }

    @Test
    void httpValidatesBodyAndReturnsNotFoundAndConflict() throws Exception {
        var p = products.create(productRequest());
        String duplicate = """
                {"maSanPham":"%s","tenSanPham":"Giày kiểm thử","danhMucId":%d,"thuongHieuId":%d,
                "chatLieuId":%d,"kieuDangId":%d,"coGiayId":%d,"xuatXuId":%d,"trangThai":1}
                """.formatted(code, ids.get("categories"), ids.get("brands"), ids.get("materials"),
                ids.get("styles"), ids.get("collars"), ids.get("origins"));
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(duplicate))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.maSanPham").exists());
        mvc.perform(get("/api/products/9223372036854775807")).andExpect(status().isNotFound());
        mvc.perform(get("/api/products").param("page", "-1")).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/products/" + p.id() + "/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"trangThai\":0}")).andExpect(status().isOk()).andExpect(jsonPath("$.trangThai").value(0));
        mvc.perform(options("/api/products").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")).andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void attributesUseRealSearchPaginationAndStatus() {
        for (String type : ids.keySet()) {
            assertEquals(1, attributes.list(type, 0, 5, code, 1).totalElements());
            var updated = attributes.save(type, ids.get(type),
                    new AttributeRequest(code, code + "-updated-" + type, "Ghi chú cập nhật",
                            type.equals("colors") ? "#ABCDEF" : null, 1));
            assertTrue(updated.ten().contains("updated"));
            attributes.changeStatus(type, ids.get(type), 0);
            assertEquals(1, attributes.list(type, 0, 5, code, 0).totalElements());
            assertEquals(0, attributes.list(type, 0, 5, code, 1).totalElements());
        }
    }
}

