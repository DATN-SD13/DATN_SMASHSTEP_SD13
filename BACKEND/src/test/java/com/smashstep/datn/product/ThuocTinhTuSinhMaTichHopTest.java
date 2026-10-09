package com.smashstep.datn.product;

import com.smashstep.datn.DatnApplication;
import com.smashstep.datn.product.service.ThuocTinhSanPhamService;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Gọi controller và SQL Server thật; mọi bản ghi kiểm thử được rollback.
@SpringBootTest(classes = DatnApplication.class, properties = {
        "spring.jpa.open-in-view=false", "spring.jpa.hibernate.ddl-auto=none", "spring.jpa.show-sql=false" })
@Transactional
class ThuocTinhTuSinhMaTichHopTest {
    @Autowired WebApplicationContext context;
    @Autowired ThuocTinhSanPhamService service;
    @Autowired EntityManager entityManager;
    private MockMvc mvc;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final String testName = "AUTO-" + UUID.randomUUID().toString().substring(0, 12);

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void sevenPrefixesCreateWithoutCodeAdvanceAndUpdatePreservesActualCode() throws Exception {
        Map<String, String> prefixes = Map.of("categories", "DM", "brands", "TH", "materials", "CL",
                "styles", "KD", "collars", "CG", "origins", "XX", "colors", "MS");
        for (var entry : prefixes.entrySet()) {
            String type = entry.getKey(), preview = service.taoMaTiepTheo(type);
            assertTrue(preview.matches(entry.getValue() + "[0-9]{3,}"));
            mvc.perform(get("/api/product-attributes/" + type + "/next-code"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.ma").value(preview));
            Map<String, Object> request = new LinkedHashMap<>(Map.of("ten", testName + "-" + type, "trangThai", 1));
            if (type.equals("colors")) request.put("maMauHex", String.format("#%06X", UUID.randomUUID().hashCode() & 0xFFFFFF));
            String body = mvc.perform(post("/api/product-attributes/" + type).contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(request)))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.ma").value(preview))
                    .andReturn().getResponse().getContentAsString();
            long id = mapper.readTree(body).get("id").asLong();
            entityManager.flush(); entityManager.clear();
            assertEquals(preview, service.layChiTiet(type, id).getMa());

            String next = service.taoMaTiepTheo(type);
            assertEquals(new java.math.BigInteger(preview.substring(2)).add(java.math.BigInteger.ONE),
                    new java.math.BigInteger(next.substring(2)));
            request.put("ma", "FORGED999"); request.put("ten", testName + "-second-" + type);
            if (type.equals("colors")) request.put("maMauHex", String.format("#%06X", UUID.randomUUID().hashCode() & 0xFFFFFF));
            mvc.perform(post("/api/product-attributes/" + type).contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(request)))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.ma").value(next));

            request.put("ma", entry.getValue() + "999"); request.put("ten", testName + "-updated-" + type);
            if (type.equals("colors")) request.put("maMauHex", service.layChiTiet(type, id).getMaMauHex());
            mvc.perform(put("/api/product-attributes/" + type + "/" + id).contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(request)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.ma").value(preview));
            entityManager.flush(); entityManager.clear();
            assertEquals(preview, service.layChiTiet(type, id).getMa());
            request.put("ten", "  " + request.get("ten").toString().toLowerCase(java.util.Locale.ROOT) + "  ");
            mvc.perform(post("/api/product-attributes/" + type).contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    @Test
    void sizesRemainWithoutCodeAndUnknownTypesReturn400() throws Exception {
        mvc.perform(get("/api/product-attributes/sizes/next-code"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Kích thước không có mã."));
        mvc.perform(get("/api/product-attributes/unknown/next-code")).andExpect(status().isBadRequest());
        String body = mvc.perform(post("/api/product-attributes/sizes").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("giaTri", testName + "-46.5", "ghiChu", "Test size", "trangThai", 1))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.ma").isEmpty())
                .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(body).get("id").asLong();
        entityManager.flush(); entityManager.clear();
        assertNull(service.layChiTiet("sizes", id).getMa());
        Number codeColumns = (Number) entityManager.createNativeQuery(
                "select count(*) from INFORMATION_SCHEMA.COLUMNS where TABLE_NAME = 'kich_thuoc' and COLUMN_NAME = 'ma_kich_thuoc'")
                .getSingleResult();
        assertEquals(0, codeColumns.intValue());
    }
}
