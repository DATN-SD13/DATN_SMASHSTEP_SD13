package com.smashstep.datn.product;

import com.smashstep.datn.DatnApplication;
import com.smashstep.datn.product.dto.DuLieuSanPham.*;
import com.smashstep.datn.product.dto.*;
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
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Kiểm thử SQL Server thật; rollback dữ liệu sau mỗi test.
@SpringBootTest(classes = DatnApplication.class, properties = {
        "spring.jpa.open-in-view=false", "spring.jpa.hibernate.ddl-auto=none", "spring.jpa.show-sql=false" })
@Transactional
class SanPhamTichHopTest {
    @Test
    void multipartUploadStoresFileAndServesItThroughRealMvcAndJpa() throws Exception {
        var product = products.themSanPham(productRequest());
        var output = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2, 2,
                java.awt.image.BufferedImage.TYPE_INT_RGB), "jpg", output);
        byte[] content = output.toByteArray();
        var file = new org.springframework.mock.web.MockMultipartFile("file", "../../ảnh sản phẩm (1).jpg", "image/jpeg", content);
        var response = mvc.perform(multipart("/api/products/" + product.getId() + "/images/upload").file(file)
                        .param("isAnhChinh", "false").header("Origin", "http://localhost:5173"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.sanPhamId").value(product.getId()))
                .andExpect(jsonPath("$.isAnhChinh").value(true))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andReturn().getResponse().getContentAsString();
        String url = JsonMapper.builder().build().readTree(response).get("urlAnh").asText();
        assertTrue(url.matches("/uploads/products/[0-9a-f-]{36}\\.jpg"));
        entityManager.flush(); entityManager.clear();
        assertEquals(url, products.layChiTietSanPham(product.getId()).getProduct().getAnhChinh());
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().bytes(content));
        mvc.perform(multipart("/api/products/" + product.getId() + "/images/upload")
                        .file(new org.springframework.mock.web.MockMultipartFile("file", "fake.jpg", "image/jpeg", new byte[20])))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Tệp ảnh không hợp lệ."));
        mvc.perform(multipart("/api/products/" + product.getId() + "/images/upload").file(file).param("mauSacId", "1"))
                .andExpect(status().isBadRequest());
        assertEquals(1, images.layDanhSachAnh(product.getId()).size());
    }
    @Autowired SanPhamService products;
    @Autowired HinhAnhSanPhamService images;
    @Autowired SanPhamChiTietService variants;
    @Autowired ThuocTinhSanPhamService attributes;
    @Autowired EntityManager entityManager;
    @Autowired WebApplicationContext context;
    private final Map<String, Long> ids = new LinkedHashMap<>();
    private String code;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        code = "TEST-" + UUID.randomUUID().toString().substring(0, 12);
        for (String type : List.of("categories", "brands", "materials", "styles", "collars", "origins", "colors", "sizes")) {
            var request = new ThuocTinhRequest(code, code + "-" + type, "Ghi chú kiểm thử",
                    type.equals("colors") ? String.format("#%06X", UUID.randomUUID().hashCode() & 0xFFFFFF) : null, 1);
            ids.put(type, attributes.luuThuocTinh(type, null, request).getId());
        }
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private SanPhamThemRequest productRequest() {
        return new SanPhamThemRequest(code, "Giày kiểm thử " + code, ids.get("categories"), ids.get("brands"),
                ids.get("materials"), ids.get("styles"), ids.get("collars"), ids.get("origins"), "Mô tả", 1);
    }

    private SanPhamChiTietThemRequest variantRequest(Long productId, Long sizeId, int quantity, String price) {
        String key = code + "-" + sizeId;
        return new SanPhamChiTietThemRequest(productId, key, key, ids.get("colors"), sizeId, quantity,
                new BigDecimal(price), true, 1);
    }

    @Test
    void persistsVariantsAndReturnsRealPaginatedAggregates() throws Exception {
        var product = products.themSanPham(productRequest());
        var size2 = attributes.luuThuocTinh("sizes", null, new ThuocTinhRequest(null, code + "-size2", "", null, 1));
        variants.themDanhSachBienThe(product.getId(), List.of(
                variantRequest(product.getId(), ids.get("sizes"), 2, "1200000"),
                variantRequest(product.getId(), size2.getId(), 3, "1500000")));
        entityManager.flush(); entityManager.clear();
        var page = products.layDanhSachSanPham(0, 1, code, 1, ids.get("categories"), ids.get("brands"),
                ids.get("materials"), ids.get("styles"), ids.get("collars"), ids.get("origins"));
        assertEquals(1, page.getTotalElements()); assertEquals(1, page.getContent().size());
        var row = page.getContent().get(0);
        assertEquals(5, row.getTongSoLuong()); assertEquals(2, row.getTongSoBienThe());
        assertEquals(1, row.getSoMau()); assertEquals(2, row.getSoKichThuoc());
        assertEquals(code + "-categories", row.getDanhMuc()); assertNull(row.getAnhChinh());
        assertEquals(0, new BigDecimal("1200000").compareTo(row.getGiaThapNhat()));
        assertEquals(0, new BigDecimal("1500000").compareTo(row.getGiaCaoNhat()));
        assertTrue(products.layDanhSachSanPham(1, 1, code, null, null, null, null, null, null, null).getContent().isEmpty());
        assertEquals(2, variants.layDanhSach(0, 10, code, 1, product.getId(), ids.get("colors"), null).getTotalElements());
        assertEquals(1, variants.layDanhSach(0, 10, code, 1, product.getId(), ids.get("colors"), size2.getId()).getTotalElements());
        mvc.perform(get("/api/products/" + product.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.product.tongSoLuong").value(5)).andExpect(jsonPath("$.variants.length()").value(2));
    }

    @Test
    void editKeepsVariantsAndImageSelectionMaintainsOneMainImage() {
        var product = products.themSanPham(productRequest());
        var variant = variants.themSanPhamChiTiet(variantRequest(product.getId(), ids.get("sizes"), 4, "1000000"));
        var updated = new SanPhamSuaRequest(code, "Tên đã sửa", ids.get("categories"), ids.get("brands"),
                ids.get("materials"), ids.get("styles"), ids.get("collars"), ids.get("origins"), "Mô tả đã sửa", 0);
        products.suaSanPham(product.getId(), updated);
        var first = images.themAnh(product.getId(), new HinhAnhSanPhamRequest("https://example.com/first.png", false));
        images.themAnh(product.getId(), new HinhAnhSanPhamRequest("https://example.com/second.png", true));
        entityManager.flush(); entityManager.clear();
        var detail = products.layChiTietSanPham(product.getId());
        assertEquals(1, detail.getVariants().size()); assertEquals(variant.getId(), detail.getVariants().get(0).getId());
        assertEquals("Tên đã sửa", detail.getProduct().getTenSanPham()); assertNotNull(detail.getProduct().getNgayCapNhat());
        assertEquals(1, detail.getImages().stream().filter(HinhAnhSanPhamResponse::getIsAnhChinh).count());
        images.suaAnh(product.getId(), first.getId(), new HinhAnhSanPhamRequest(first.getUrlAnh(), true));
        variants.suaSanPhamChiTiet(variant.getId(), new SanPhamChiTietSuaRequest(product.getId(), variant.getMaChiTietSanPham(), variant.getSku(),
                variant.getMauSacId(), variant.getKichThuocId(), 7, new BigDecimal("1100000"), false, 0));
        entityManager.flush(); entityManager.clear();
        var refreshed = products.layChiTietSanPham(product.getId());
        assertEquals(7, refreshed.getProduct().getTongSoLuong());
        assertEquals(1, refreshed.getImages().stream().filter(HinhAnhSanPhamResponse::getIsAnhChinh).count());
        assertTrue(refreshed.getImages().stream().filter(i -> i.getId().equals(first.getId())).findFirst().orElseThrow().getIsAnhChinh());
        assertFalse(refreshed.getVariants().get(0).getKichHoat());
        assertEquals(first.getUrlAnh(), refreshed.getProduct().getAnhChinh());
        assertEquals(first.getUrlAnh(), refreshed.getVariants().get(0).getAnhChinh());
        assertEquals(first.getUrlAnh(), variants.layTheoSanPham(product.getId()).get(0).getAnhChinh());
    }

    @Test
    void httpValidatesBodyAndReturnsNotFoundAndConflict() throws Exception {
        var p = products.themSanPham(productRequest());
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
        mvc.perform(patch("/api/products/" + p.getId() + "/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"trangThai\":0}")).andExpect(status().isOk()).andExpect(jsonPath("$.trangThai").value(0));
        mvc.perform(options("/api/products").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")).andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void attributesUseRealSearchPaginationAndStatus() {
        for (String type : ids.keySet()) {
            assertEquals(1, attributes.layDanhSach(type, 0, 5, code, 1).getTotalElements());
            var updated = attributes.luuThuocTinh(type, ids.get(type),
                    new ThuocTinhRequest(code, code + "-updated-" + type, "Ghi chú cập nhật",
                            type.equals("colors") ? "#ABCDEF" : null, 1));
            assertTrue(updated.getTen().contains("updated"));
            attributes.doiTrangThai(type, ids.get(type), 0);
            assertEquals(1, attributes.layDanhSach(type, 0, 5, code, 0).getTotalElements());
            assertEquals(0, attributes.layDanhSach(type, 0, 5, code, 1).getTotalElements());
        }
    }

    @Test
    void httpCrudWritesRealRowsAndReturnsProductVariantImages() throws Exception {
        JsonMapper mapper = JsonMapper.builder().build();
        String body = mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(productRequest())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.maSanPham").value(code))
                .andExpect(jsonPath("$.ngayTao").exists()).andExpect(jsonPath("$.soMau").value(0))
                .andReturn().getResponse().getContentAsString();
        Long productId = ((Number) mapper.readValue(body, Map.class).get("id")).longValue();
        String variantBody = mapper.writeValueAsString(Map.of("variants", List.of(variantRequest(productId, ids.get("sizes"), 3, "1200000"))))
                .replace("\"sanPhamId\"", "\"idSanPham\"").replace("\"mauSacId\"", "\"idMauSac\"")
                .replace("\"kichThuocId\"", "\"idKichThuoc\"");
        mvc.perform(post("/api/products/" + productId + "/variants").contentType(MediaType.APPLICATION_JSON)
                        .content(variantBody))
                .andExpect(status().isCreated()).andExpect(jsonPath("$[0].sanPhamId").value(productId))
                .andExpect(jsonPath("$[0].idSanPham").value(productId));
        mvc.perform(post("/api/products/" + productId + "/images").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new HinhAnhSanPhamRequest("https://example.com/product.png", true))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.isAnhChinh").value(true));
        Long variantId = variants.layTheoSanPham(productId).get(0).getId();
        SanPhamChiTietThemRequest editedVariant = variantRequest(productId, ids.get("sizes"), 8, "1500000");
        editedVariant.setKichHoat(false);
        mvc.perform(put("/api/product-details/" + variantId).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(editedVariant)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.soLuong").value(8))
                .andExpect(jsonPath("$.kichHoat").value(false));
        SanPhamThemRequest editedProduct = productRequest();
        editedProduct.setTenSanPham("  Tên mới qua HTTP  ");
        mvc.perform(put("/api/products/" + productId).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(editedProduct)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tenSanPham").value("Tên mới qua HTTP"))
                .andExpect(jsonPath("$.ngayCapNhat").exists());
        mvc.perform(patch("/api/product-details/" + variantId + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trangThai\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.trangThai").value(0));
        mvc.perform(patch("/api/products/" + productId + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trangThai\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.trangThai").value(0));
        entityManager.flush(); entityManager.clear();
        mvc.perform(get("/api/products").param("keyword", code).param("status", "0"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].tongSoLuong").value(8))
                .andExpect(jsonPath("$.content[0].soMau").value(1))
                .andExpect(jsonPath("$.content[0].soKichThuoc").value(1))
                .andExpect(jsonPath("$.content[0].anhChinh").value("https://example.com/product.png"));
        mvc.perform(get("/api/products/" + productId + "/variants"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(variantId))
                .andExpect(jsonPath("$[0].giaBan").value(1500000))
                .andExpect(jsonPath("$[0].anhChinh").value("https://example.com/product.png"));
        mvc.perform(get("/api/product-details").param("productId", productId.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].trangThai").value(0))
                .andExpect(jsonPath("$.content[0].anhChinh").value("https://example.com/product.png"));
        mvc.perform(get("/api/products/9223372036854775807/variants")).andExpect(status().isNotFound());
        Long imageId = products.layChiTietSanPham(productId).getImages().get(0).getId();
        mvc.perform(put("/api/products/" + productId + "/images/" + imageId).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new HinhAnhSanPhamRequest("https://example.com/updated.png", true))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.urlAnh").value("https://example.com/updated.png"));
        mvc.perform(delete("/api/products/" + productId + "/images/" + imageId)).andExpect(status().isNoContent());
        entityManager.flush(); entityManager.clear();
        assertTrue(products.layChiTietSanPham(productId).getImages().isEmpty());
        assertNull(products.layChiTietSanPham(productId).getProduct().getAnhChinh());
        mvc.perform(post("/api/products/" + productId + "/images").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new HinhAnhSanPhamRequest("data:image/png;base64,abc", true))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void allEightAttributeGetEndpointsReadConfiguredDatabase() throws Exception {
        for (String type : ids.keySet()) {
            mvc.perform(get("/api/product-attributes/" + type).param("keyword", code).param("status", "1"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(ids.get(type)));
        }
        List<?> columns = entityManager.createNativeQuery(
                "select COLUMN_NAME from INFORMATION_SCHEMA.COLUMNS where TABLE_SCHEMA = 'dbo' "
                        + "and TABLE_NAME = 'hinh_anh_san_pham' order by ORDINAL_POSITION").getResultList();
        assertEquals(List.of("id", "id_san_pham", "url_anh", "is_anh_chinh", "id_san_pham_chi_tiet"), columns);
    }

    @Test
    void allEightAttributeCrudEndpointsWriteRealRows() throws Exception {
        JsonMapper mapper = JsonMapper.builder().build();
        String attributeCode = code + "-HTTP";
        for (String type : ids.keySet()) {
            ThuocTinhRequest request = new ThuocTinhRequest(attributeCode, attributeCode + "-" + type,
                    "Ghi chú qua HTTP", type.equals("colors") ? String.format("#%06X", UUID.randomUUID().hashCode() & 0xFFFFFF) : null, 1);
            String body = mvc.perform(post("/api/product-attributes/" + type).contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(request)))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.trangThai").value(1))
                    .andReturn().getResponse().getContentAsString();
            Long id = ((Number) mapper.readValue(body, Map.class).get("id")).longValue();
            request.setTen(attributeCode + "-edit-" + type);
            mvc.perform(put("/api/product-attributes/" + type + "/" + id).contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(request)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.ten").value(request.getTen()));
            mvc.perform(patch("/api/product-attributes/" + type + "/" + id + "/status")
                            .contentType(MediaType.APPLICATION_JSON).content("{\"trangThai\":0}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.trangThai").value(0));
            entityManager.flush(); entityManager.clear();
            mvc.perform(get("/api/product-attributes/" + type).param("keyword", attributeCode).param("status", "0"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(id));
            mvc.perform(get("/api/product-attributes/" + type).param("keyword", attributeCode).param("status", "1"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    @Test
    void postmanSingleVariantAndUpdatesDoNotRequireImmutableIdsOrCodes() throws Exception {
        JsonMapper mapper = JsonMapper.builder().build();
        SanPhamThemRequest create = productRequest();
        String createBody = mapper.writeValueAsString(create)
                .replace("\"maSanPham\":\"" + code + "\"", "\"maSanPham\":\"  " + code + "  \"");
        Long productId = ((Number) mapper.readValue(mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.maSanPham").value(code))
                .andExpect(jsonPath("$.soBienThe").value(0))
                .andReturn().getResponse().getContentAsString(), Map.class).get("id")).longValue();
        Map<String, Object> single = new LinkedHashMap<>(Map.of(
                "mauSacId", ids.get("colors"), "kichThuocId", ids.get("sizes"),
                "maChiTietSanPham", "  " + code + "-CT  ", "sku", "  " + code + "-SKU  ",
                "soLuong", 10, "giaBan", 1200000, "kichHoat", true, "trangThai", 1));
        Long variantId = ((Number) mapper.readValue(mvc.perform(post("/api/products/" + productId + "/variants")
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(single)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.maChiTietSanPham").value(code + "-CT"))
                .andExpect(jsonPath("$.sku").value(code + "-SKU"))
                .andReturn().getResponse().getContentAsString(), Map.class).get("id")).longValue();
        mvc.perform(get("/api/product-details/" + variantId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.maMauSac").value(attributes.layChiTiet("colors", ids.get("colors")).getMa()));
        single.remove("maChiTietSanPham");
        single.put("soLuong", 12); single.put("kichHoat", false);
        mvc.perform(put("/api/product-details/" + variantId).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(single)))
                .andExpect(result -> assertEquals(200, result.getResponse().getStatus(), result.getResponse().getContentAsString()))
                .andExpect(jsonPath("$.maChiTietSanPham").value(code + "-CT"))
                .andExpect(jsonPath("$.ngayCapNhat").exists());
        Map<String, Object> update = new LinkedHashMap<>(mapper.readValue(mapper.writeValueAsString(create), Map.class));
        update.remove("maSanPham"); update.put("tenSanPham", "  Tên sửa Postman  ");
        mvc.perform(put("/api/products/" + productId).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(update)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.maSanPham").value(code))
                .andExpect(jsonPath("$.tenSanPham").value("Tên sửa Postman"));
        entityManager.flush(); entityManager.clear();
        mvc.perform(get("/api/product-details").param("productId", productId.toString()).param("active", "false"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/product-details").param("productId", productId.toString()).param("active", "true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/products/" + productId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.product.soBienThe").value(1))
                .andExpect(jsonPath("$.product.tongSoLuong").value(12));
        var size2 = attributes.luuThuocTinh("sizes", null, new ThuocTinhRequest(null, code + "-second", "", null, 1));
        single.put("maChiTietSanPham", code + "-CT2"); single.put("sku", code + "-SKU2");
        single.put("kichThuocId", size2.getId());
        mvc.perform(post("/api/products/" + productId + "/variants").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("variants", List.of(single)))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$[0].sanPhamId").value(productId));
    }

    @Test
    void postmanAttributeDetailsAndNativeFieldAliasesUseRealEntityFields() throws Exception {
        for (String type : ids.keySet()) {
            mvc.perform(get("/api/product-attributes/" + type + "/" + ids.get(type)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(ids.get(type)));
            mvc.perform(get("/api/product-attributes/" + type + "/9223372036854775807"))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").exists());
        }
        JsonMapper mapper = JsonMapper.builder().build();
        String maMauMoi = attributes.taoMaTiepTheo("colors");
        mvc.perform(post("/api/product-attributes/colors").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("maMauSac", code + "-MC", "tenMauSac", code + "-Màu",
                                "maMauHex", "#AABBCC", "trangThai", 1))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.ma").value(maMauMoi));
        mvc.perform(post("/api/product-attributes/sizes").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("giaTri", code + "-40.5", "ghiChu", "Size dạng chuỗi", "trangThai", 1))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.ten").value(code + "-40.5"));
        mvc.perform(get("/api/product-attributes/invalid/1")).andExpect(status().isBadRequest());
    }

    @Test
    void postmanImagePathsMaintainOneMainImageAndNeverDeleteProduct() throws Exception {
        var product = products.themSanPham(productRequest());
        var first = images.themAnh(product.getId(), new HinhAnhSanPhamRequest("https://example.com/one.png", false));
        var second = images.themAnh(product.getId(), new HinhAnhSanPhamRequest("https://example.com/two.png", false));
        mvc.perform(get("/api/products/" + product.getId() + "/images"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(put("/api/product-images/" + second.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"urlAnh\":\"https://example.com/updated.png\",\"isAnhChinh\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.isAnhChinh").value(true));
        entityManager.flush(); entityManager.clear();
        assertEquals(1, images.layDanhSachAnh(product.getId()).stream().filter(HinhAnhSanPhamResponse::getIsAnhChinh).count());
        mvc.perform(delete("/api/product-images/" + second.getId())).andExpect(status().isNoContent());
        entityManager.flush(); entityManager.clear();
        assertTrue(images.layDanhSachAnh(product.getId()).get(0).getIsAnhChinh());
        assertEquals(first.getId(), images.layDanhSachAnh(product.getId()).get(0).getId());
        assertEquals(product.getId(), products.layChiTietSanPham(product.getId()).getProduct().getId());
        mvc.perform(post("/api/products/" + product.getId() + "/images").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"urlAnh\":\"https://example.com/color.png\",\"isAnhChinh\":true,\"mauSacId\":1}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.mauSacId").exists());
        mvc.perform(get("/api/products/9223372036854775807/images")).andExpect(status().isNotFound());
        mvc.perform(put("/api/product-images/9223372036854775807").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"urlAnh\":\"https://example.com/none.png\",\"isAnhChinh\":true}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void postmanRejectsInvalidVariantsAndForeignKeysWithoutSavingRows() throws Exception {
        JsonMapper mapper = JsonMapper.builder().build();
        var product = products.themSanPham(productRequest());
        Map<String, Object> body = new LinkedHashMap<>(Map.of(
                "mauSacId", ids.get("colors"), "kichThuocId", ids.get("sizes"),
                "maChiTietSanPham", code + "-CT", "sku", code + "-SKU",
                "soLuong", 1, "giaBan", 100000, "kichHoat", true, "trangThai", 1));
        mvc.perform(post("/api/product-details").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.sanPhamId").exists());
        body.put("soLuong", -1); body.put("giaBan", 0);
        mvc.perform(post("/api/products/" + product.getId() + "/variants").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors['variants[0].soLuong']").exists())
                .andExpect(jsonPath("$.errors['variants[0].giaBan']").exists());
        body.put("soLuong", 1); body.put("giaBan", 100000); body.put("mauSacId", Long.MAX_VALUE);
        mvc.perform(post("/api/products/" + product.getId() + "/variants").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body))).andExpect(status().isNotFound());
        body.put("mauSacId", ids.get("colors"));
        mvc.perform(post("/api/products/" + product.getId() + "/variants").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body))).andExpect(status().isCreated());
        body.put("maChiTietSanPham", code + "-CT2");
        mvc.perform(post("/api/products/" + product.getId() + "/variants").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("SKU đã tồn tại"));
        body.put("sku", code + "-SKU2");
        mvc.perform(post("/api/products/" + product.getId() + "/variants").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body))).andExpect(status().isConflict());
        mvc.perform(get("/api/product-details/9223372036854775807")).andExpect(status().isNotFound());
        body.put("sanPhamId", Long.MAX_VALUE);
        mvc.perform(post("/api/products/" + product.getId() + "/variants").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/product-details").param("active", "invalid")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/products/" + product.getId() + "/variants").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variants\":[]}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/products/" + product.getId() + "/variants").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variants\":[null]}")).andExpect(status().isBadRequest());
        SanPhamThemRequest invalidFk = productRequest();
        invalidFk.setMaSanPham(code + "-FK"); invalidFk.setDanhMucId(Long.MAX_VALUE);
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(invalidFk))).andExpect(status().isNotFound());
        body.put("sanPhamId", product.getId()); body.put("giaBan", 0);
        mvc.perform(post("/api/product-details").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.giaBan").exists());
        mvc.perform(delete("/api/products/" + product.getId())).andExpect(status().isMethodNotAllowed());
        assertEquals(1, variants.layTheoSanPham(product.getId()).size());
    }

    @Test
    void thuocTinhKiemTraTrungTenVaGiaTriKhiMaDoBackendSinh() throws Exception {
        JsonMapper mapper = JsonMapper.builder().build();
        for (String loai : ids.keySet()) {
            ThuocTinhResponse hienTai = attributes.layChiTiet(loai, ids.get(loai));
            ThuocTinhRequest trung = new ThuocTinhRequest(hienTai.getMa(), hienTai.getTen(),
                    "", hienTai.getMaMauHex(), 1);
            mvc.perform(post("/api/product-attributes/" + loai).contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(trung)))
                    .andExpect(status().isConflict());

            if (!loai.equals("sizes")) {
                ThuocTinhRequest cungTen = new ThuocTinhRequest(code + "-moi", hienTai.getTen(),
                        "", hienTai.getMaMauHex(), 1);
                mvc.perform(post("/api/product-attributes/" + loai).contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(cungTen)))
                        .andExpect(status().isConflict());
            }
            mvc.perform(put("/api/product-attributes/" + loai + "/9223372036854775807")
                            .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(trung)))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void khoaNgoaiKhongTonTaiTra404() throws Exception {
        JsonMapper mapper = JsonMapper.builder().build();
        Map<String, Object> yeuCau = new LinkedHashMap<>(Map.of(
                "maSanPham", code + "-FK", "tenSanPham", "Sản phẩm kiểm thử FK",
                "danhMucId", ids.get("categories"), "thuongHieuId", ids.get("brands"),
                "chatLieuId", ids.get("materials"), "kieuDangId", ids.get("styles"),
                "coGiayId", ids.get("collars"), "xuatXuId", ids.get("origins"), "trangThai", 1));
        Map<String, String> cacKhoaNgoai = Map.of(
                "danhMucId", "categories", "thuongHieuId", "brands",
                "chatLieuId", "materials", "kieuDangId", "styles",
                "coGiayId", "collars", "xuatXuId", "origins");
        for (var khoaNgoai : cacKhoaNgoai.entrySet()) {
            yeuCau.put(khoaNgoai.getKey(), Long.MAX_VALUE);
            mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(yeuCau)))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").exists());
            yeuCau.put(khoaNgoai.getKey(), ids.get(khoaNgoai.getValue()));
        }

        SanPhamResponse sanPham = products.themSanPham(productRequest());
        SanPhamChiTietThemRequest bienThe = variantRequest(sanPham.getId(), ids.get("sizes"), 1, "1200000");
        bienThe.setMauSacId(Long.MAX_VALUE);
        mvc.perform(post("/api/product-details").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(bienThe)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Không tìm thấy màu sắc"));
        bienThe.setMauSacId(ids.get("colors"));
        bienThe.setKichThuocId(Long.MAX_VALUE);
        mvc.perform(post("/api/product-details").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(bienThe)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Không tìm thấy kích thước"));
        bienThe.setKichThuocId(ids.get("sizes"));
        bienThe.setSanPhamId(Long.MAX_VALUE);
        mvc.perform(post("/api/product-details").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(bienThe)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Không tìm thấy sản phẩm"));
        assertTrue(variants.layTheoSanPham(sanPham.getId()).isEmpty());
    }
    @Test
    void exactVariantImagesPersistAndMainSelectionNeverCrossesScopes() throws Exception {
        var product = products.themSanPham(productRequest());
        var size2 = attributes.luuThuocTinh("sizes", null, new ThuocTinhRequest(null, code + "-second-size", "", null, 1));
        var a = variants.themSanPhamChiTiet(variantRequest(product.getId(), ids.get("sizes"), 2, "1000000"));
        var b = variants.themSanPhamChiTiet(variantRequest(product.getId(), size2.getId(), 3, "1200000"));
        var common = images.themAnh(product.getId(), new HinhAnhSanPhamRequest("https://example.com/common.png", true));
        var output = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", output);
        var file = new org.springframework.mock.web.MockMultipartFile("file", "variant.png", "image/png", output.toByteArray());
        var a1 = images.taiAnhBienThe(a.getId(), file, false);
        var a2 = images.taiAnhBienThe(a.getId(), file, true);
        var b1 = images.taiAnhBienThe(b.getId(), file, false);
        entityManager.flush(); entityManager.clear();
        assertEquals(a.getId(), a1.getSanPhamChiTietId());
        assertEquals(product.getId(), a1.getSanPhamId());
        assertNotEquals(a2.getUrlAnh(), b1.getUrlAnh());
        assertEquals(1, images.layDanhSachAnh(product.getId()).size());
        assertEquals(common.getUrlAnh(), products.layChiTietSanPham(product.getId()).getProduct().getAnhChinh());
        assertEquals(a2.getUrlAnh(), variants.layChiTiet(a.getId()).getAnhChinh());
        assertEquals(b1.getUrlAnh(), variants.layChiTiet(b.getId()).getAnhChinh());
        mvc.perform(get("/api/product-details/" + a.getId() + "/images"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(put("/api/product-details/" + b.getId() + "/images/" + a1.getId())
                .contentType(MediaType.APPLICATION_JSON).content("{\"urlAnh\":\"" + a1.getUrlAnh() + "\",\"isAnhChinh\":true}"))
                .andExpect(status().isNotFound());
        images.suaAnhTheoId(a1.getId(), new HinhAnhSanPhamRequest(a1.getUrlAnh(), true));
        entityManager.flush(); entityManager.clear();
        assertEquals(a1.getUrlAnh(), variants.layChiTiet(a.getId()).getAnhChinh());
        assertEquals(b1.getUrlAnh(), variants.layChiTiet(b.getId()).getAnhChinh());
        assertEquals(common.getUrlAnh(), images.layDanhSachAnh(product.getId()).get(0).getUrlAnh());
        var detail = products.layChiTietSanPham(product.getId());
        assertEquals(a1.getUrlAnh(), detail.getVariants().stream().filter(v -> v.getId().equals(a.getId())).findFirst().orElseThrow().getAnhChinh());
        var page = variants.layDanhSach(0, 10, code, null, product.getId(), null, null);
        assertEquals(b1.getUrlAnh(), page.getContent().stream().filter(v -> v.getId().equals(b.getId())).findFirst().orElseThrow().getAnhChinh());
        images.xoaAnhBienThe(a.getId(), a1.getId());
        entityManager.flush(); entityManager.clear();
        assertEquals(a2.getUrlAnh(), variants.layChiTiet(a.getId()).getAnhChinh());
        images.xoaAnhTheoId(a2.getId());
        entityManager.flush(); entityManager.clear();
        assertEquals(common.getUrlAnh(), variants.layChiTiet(a.getId()).getAnhChinh());
        assertEquals(b1.getUrlAnh(), variants.layChiTiet(b.getId()).getAnhChinh());
        mvc.perform(multipart("/api/product-details/" + b.getId() + "/images/upload").file(file)
                        .param("sanPhamId", "999999").param("isAnhChinh", "false"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.sanPhamId").value(product.getId()))
                .andExpect(jsonPath("$.sanPhamChiTietId").value(b.getId()));
        assertThrows(com.smashstep.datn.common.exception.AppException.class,
                () -> images.xoaAnh(product.getId(), b1.getId()));
    }

    @Test
    void allSixInactiveProductAttributesRejectNewAssignmentsButKeepHistoricalEdits() throws Exception {
        var original = products.themSanPham(productRequest());
        var mapper = JsonMapper.builder().build();
        for (String type : List.of("categories", "brands", "materials", "styles", "collars", "origins")) {
            attributes.doiTrangThai(type, ids.get(type), 0);
            var request = productRequest(); request.setMaSanPham(code + "-new"); request.setTenSanPham("New " + code + type);
            mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
            var edit = new SanPhamSuaRequest(original.getMaSanPham(), "Giữ thuộc tính cũ", ids.get("categories"), ids.get("brands"),
                    ids.get("materials"), ids.get("styles"), ids.get("collars"), ids.get("origins"), "", 1);
            mvc.perform(put("/api/products/" + original.getId()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(edit)))
                    .andExpect(status().isOk());
            attributes.doiTrangThai(type, ids.get(type), 1);
        }
    }

    @Test
    void inactiveColorsAndSizesRejectNewVariantsButAllowUnchangedHistoricalEdit() throws Exception {
        var product = products.themSanPham(productRequest());
        var existing = variants.themSanPhamChiTiet(variantRequest(product.getId(), ids.get("sizes"), 2, "1000000"));
        var request2 = productRequest(); request2.setMaSanPham(code + "-new-parent"); request2.setTenSanPham("Other parent " + code);
        var product2 = products.themSanPham(request2);
        var mapper = JsonMapper.builder().build();
        for (String type : List.of("colors", "sizes")) {
            attributes.doiTrangThai(type, ids.get(type), 0);
            var newVariant = variantRequest(product2.getId(), ids.get("sizes"), 1, "1000000");
            newVariant.setMaChiTietSanPham(code + "-new-" + type);
            newVariant.setSku(code + "-new-" + type);
            mvc.perform(post("/api/product-details").contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(newVariant)))
                    .andExpect(status().isBadRequest());
            var edit = new SanPhamChiTietSuaRequest(product.getId(), existing.getMaChiTietSanPham(), existing.getSku(),
                    ids.get("colors"), ids.get("sizes"), 5, new BigDecimal("1100000"), true, 1);
            mvc.perform(put("/api/product-details/" + existing.getId()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(edit)))
                    .andExpect(status().isOk());
            attributes.doiTrangThai(type, ids.get(type), 1);
        }
    }

}
