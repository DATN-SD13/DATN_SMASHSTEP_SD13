package com.smashstep.datn.customer;

import com.smashstep.datn.DatnApplication;
import com.smashstep.datn.customer.entity.KhachHang;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Các API chạy trên SQL Server thật trong transaction rollback, không sửa schema/dữ liệu cũ.
@SpringBootTest(classes = DatnApplication.class, properties = {
        "spring.jpa.open-in-view=false", "spring.jpa.hibernate.ddl-auto=none", "spring.jpa.show-sql=false" })
@Transactional
class KhachHangTichHopTest {
    @Autowired WebApplicationContext context;
    @Autowired EntityManager entityManager;
    private MockMvc mvc;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final String token = UUID.randomUUID().toString().substring(0, 12);
    private final String phone = "09" + String.format("%08d", Math.floorMod(UUID.randomUUID().hashCode(), 100_000_000));

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private Map<String, Object> address(boolean isDefault) {
        return Map.of("receiverName", "Khách kiểm thử", "receiverPhone", phone,
                "province", "Hà Nội", "ward", "Phường Ba Đình", "street", "10 Đường kiểm thử",
                "addressType", 1, "isDefault", isDefault);
    }

    private Map<String, Object> request() {
        return new LinkedHashMap<>(Map.of("name", "Khách kiểm thử " + token,
                "email", "customer-" + token + "@example.test", "phone", phone,
                "gender", 1, "dob", "2000-01-01", "defaultAddress", address(true)));
    }

    private JsonNode data(String body) {
        return mapper.readTree(body).get("data");
    }

    @Test
    void customerCrudSearchPaginationStatusAndAddressDefaultsMatchFrontendContract() throws Exception {
        Map<String, Object> request = request();
        JsonNode created = data(mvc.perform(post("/api/khach-hang").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.matKhau").doesNotExist())
                .andReturn().getResponse().getContentAsString());
        String code = created.get("code").asString();
        long customerId = created.get("id").asLong();
        long firstAddressId = created.get("defaultAddress").get("id").asLong();
        assertEquals(code, created.get("username").asString());
        assertTrue(code.matches("KH[0-9]{4,}"));

        // Seed cũ có thể trùng số điện thoại; sửa thông tin khác vẫn phải giữ được số cũ.
        KhachHang legacyDuplicate = new KhachHang();
        legacyDuplicate.setMaKhachHang("LEGACY-" + token);
        legacyDuplicate.setTenKhachHang("Khách dữ liệu cũ");
        legacyDuplicate.setEmail("legacy-" + token + "@example.test");
        legacyDuplicate.setSoDienThoai(phone);
        legacyDuplicate.setTrangThai(1);
        entityManager.persist(legacyDuplicate);
        entityManager.flush();

        mvc.perform(get("/api/khach-hang").param("tuKhoa", code).param("trangThai", "1")
                        .param("page", "1").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].code").value(code))
                .andExpect(jsonPath("$.data.totalElements").value(1));
        request.put("name", "Khách đã cập nhật " + token);
        request.remove("defaultAddress");
        mvc.perform(put("/api/khach-hang/" + code).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value(request.get("name")));

        JsonNode secondAddress = data(mvc.perform(post("/api/khach-hang/" + code + "/dia-chi")
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(address(true))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        long secondAddressId = secondAddress.get("id").asLong();
        entityManager.flush();
        assertEquals(1L, entityManager.createQuery("select count(d) from DiaChiKhachHang d "
                        + "where d.idKhachHang.id = :id and d.trangThai = 1 and d.isMacDinh = true", Long.class)
                .setParameter("id", customerId).getSingleResult());
        mvc.perform(patch("/api/khach-hang/" + code + "/dia-chi/" + firstAddressId + "/mac-dinh"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.isDefault").value(true));
        mvc.perform(patch("/api/khach-hang/" + code + "/dia-chi/" + firstAddressId + "/ngung-hoat-dong"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/khach-hang/" + code + "/dia-chi"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(secondAddressId))
                .andExpect(jsonPath("$.data[0].isDefault").value(true));
        mvc.perform(patch("/api/khach-hang/" + code + "/trang-thai").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.active").value(false));
        mvc.perform(get("/api/khach-hang/" + code))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value(0))
                .andExpect(jsonPath("$.data.matKhau").doesNotExist());
        mvc.perform(patch("/api/khach-hang/" + code + "/trang-thai").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    void validationDuplicateEmailAndPasswordSerializationAreSafe() throws Exception {
        KhachHang customer = new KhachHang();
        customer.setMatKhau("test-secret-never-serialized");
        String serialized = mapper.writeValueAsString(customer);
        assertFalse(serialized.contains("matKhau"));
        assertFalse(serialized.contains("test-secret"));
        Map<String, Object> request = request();
        String code = data(mvc.perform(post("/api/khach-hang").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("code").asString();
        mvc.perform(patch("/api/khach-hang/" + code + "/trang-thai").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":2}"))
                .andExpect(status().isBadRequest());
        Map<String, Object> invalidAddress = new LinkedHashMap<>(address(false));
        invalidAddress.put("addressType", 3);
        mvc.perform(post("/api/khach-hang/" + code + "/dia-chi").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(invalidAddress)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/khach-hang").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
