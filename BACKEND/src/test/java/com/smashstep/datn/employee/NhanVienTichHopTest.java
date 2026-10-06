package com.smashstep.datn.employee;

import com.smashstep.datn.DatnApplication;
import com.smashstep.datn.employee.entity.NhanVien;
import com.smashstep.datn.employee.entity.VaiTro;
import com.smashstep.datn.employee.repository.VaiTroRepository;
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

@SpringBootTest(classes = DatnApplication.class, properties = {
        "spring.jpa.open-in-view=false", "spring.jpa.hibernate.ddl-auto=none", "spring.jpa.show-sql=false" })
@Transactional
class NhanVienTichHopTest {
    @Autowired WebApplicationContext context;
    @Autowired VaiTroRepository roles;
    private MockMvc mvc;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final String token = UUID.randomUUID().toString().substring(0, 12);
    private final String phone = "08" + String.format("%08d", Math.floorMod(UUID.randomUUID().hashCode(), 100_000_000));

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private Map<String, Object> request() {
        VaiTro role = roles.findByTrangThaiOrderByIdAsc(1).stream().findFirst().orElseGet(() -> {
            VaiTro value = new VaiTro();
            value.setTenVaiTro("Vai trò kiểm thử " + token);
            value.setTrangThai(1);
            return roles.saveAndFlush(value);
        });
        return new LinkedHashMap<>(Map.of("name", "Nhân viên kiểm thử " + token,
                "email", "employee-" + token + "@example.test", "phone", phone,
                "roleId", role.getId(), "gender", 1, "dob", "2000-01-01",
                "province", "Hà Nội", "ward", "Phường Ba Đình", "street", "10 Đường kiểm thử"));
    }

    private JsonNode data(String body) {
        return mapper.readTree(body).get("data");
    }

    @Test
    void employeeCrudRolesSearchFiltersPaginationAndStatusMatchFrontendContract() throws Exception {
        Map<String, Object> request = request();
        JsonNode created = data(mvc.perform(post("/api/nhan-vien").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.matKhau").doesNotExist())
                .andReturn().getResponse().getContentAsString());
        String code = created.get("code").asString();
        assertEquals(code, created.get("username").asString());
        assertTrue(code.matches("NV[0-9]{4,}"));
        mvc.perform(get("/api/nhan-vien/vai-tro"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());
        mvc.perform(get("/api/nhan-vien").param("tuKhoa", code).param("vaiTroId", request.get("roleId").toString())
                        .param("trangThai", "1").param("page", "1").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].code").value(code))
                .andExpect(jsonPath("$.data.totalElements").value(1));
        request.put("name", "Nhân viên đã cập nhật " + token);
        mvc.perform(put("/api/nhan-vien/" + code).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value(request.get("name")));
        for (int status : new int[]{0, 1}) {
            mvc.perform(patch("/api/nhan-vien/" + code + "/trang-thai").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":" + status + "}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.active").value(status == 1));
        }
        mvc.perform(get("/api/nhan-vien/" + code))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.roleId").value(request.get("roleId")))
                .andExpect(jsonPath("$.data.matKhau").doesNotExist());
    }

    @Test
    void requestLengthsStatusDuplicateEmailAndPasswordSerializationAreSafe() throws Exception {
        NhanVien employee = new NhanVien();
        employee.setMatKhau("test-secret-never-serialized");
        String serialized = mapper.writeValueAsString(employee);
        assertFalse(serialized.contains("matKhau"));
        assertFalse(serialized.contains("test-secret"));
        Map<String, Object> request = request();
        String code = data(mvc.perform(post("/api/nhan-vien").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("code").asString();
        mvc.perform(patch("/api/nhan-vien/" + code + "/trang-thai").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":2}"))
                .andExpect(status().isBadRequest());
        request.put("street", "X".repeat(501));
        mvc.perform(put("/api/nhan-vien/" + code).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        request.put("street", "10 Đường kiểm thử");
        mvc.perform(post("/api/nhan-vien").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
