package com.smashstep.datn.promotion;

import com.smashstep.datn.DatnApplication;
import com.smashstep.datn.customer.entity.KhachHang;
import com.smashstep.datn.promotion.service.DotGiamGiaService;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// SQL Server thật, rollback mọi voucher/assignment/campaign/customer fixture.
// Sản phẩm và biến thể hiện có chỉ được đọc.
@SpringBootTest(classes = DatnApplication.class, properties = {
        "spring.jpa.open-in-view=false", "spring.jpa.hibernate.ddl-auto=none", "spring.jpa.show-sql=false" })
@Transactional
class PromotionTichHopTest {
    @Autowired WebApplicationContext context;
    @Autowired EntityManager entityManager;
    @Autowired DotGiamGiaService campaigns;
    @Autowired com.smashstep.datn.common.config.DatabaseCapabilities database;
    private MockMvc mvc;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final String testCode = "PROMO-" + UUID.randomUUID().toString().substring(0, 12);

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private Map<String, Object> voucherRequest() {
        var request = new LinkedHashMap<String, Object>(Map.of("code", testCode, "name", testCode, "form", 1,
                "discountType", 1, "discountValue", 15, "quantity", 10, "status", 1,
                "startDate", "2097-01-01T08:00", "endDate", "2097-01-31T23:59"));
        request.put("minOrderValue", 0);
        request.put("maxDiscount", 100000);
        return request;
    }

    private void clear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void privateVoucherPersistsRecipientsAndFiltersThenEditPublicPreservesRelationAndStatus() throws Exception {
        var customer = new KhachHang();
        customer.setMaKhachHang(testCode);
        customer.setTenTaiKhoan(testCode);
        customer.setTenKhachHang("Khách kiểm thử " + testCode);
        customer.setTrangThai(1);
        entityManager.persist(customer);
        entityManager.flush();
        Long customerId = customer.getId();
        Map<String, Object> request = voucherRequest();
        request.put("form", 2);
        request.put("customerIds", List.of(customerId, customerId));
        request.put("quantity", 1);
        if (!database.hasColumn("phieu_giam_gia", "hinh_thuc_phieu")) {
            mvc.perform(post("/api/phieu-giam-gia").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
            mvc.perform(get("/api/phieu-giam-gia/capabilities")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.formSupported").value(false));
            mvc.perform(get("/api/phieu-giam-gia").param("hinhThuc", "2")).andExpect(status().isBadRequest());
            return;
        }
        String body = mvc.perform(post("/api/phieu-giam-gia").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.form").value(2))
                .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(body).get("data").get("id").asLong();
        clear();
        mvc.perform(get("/api/phieu-giam-gia/" + id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.form").value(2))
                .andExpect(jsonPath("$.data.customerIds[0]").value(customerId));
        mvc.perform(get("/api/phieu-giam-gia").param("ma", testCode).param("hinhThuc", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
        mvc.perform(get("/api/phieu-giam-gia").param("ma", testCode).param("hinhThuc", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));

        request.put("name", testCode + " updated");
        mvc.perform(put("/api/phieu-giam-gia/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.form").value(2));
        clear();
        Number assignmentCount = (Number) entityManager.createNativeQuery(
                        "select count(*) from phieu_giam_gia_khach_hang where id_phieu_giam_gia=:id")
                .setParameter("id", id).getSingleResult();
        assertEquals(1, assignmentCount.intValue());

        request.put("form", 1);
        request.put("customerIds", List.of());
        mvc.perform(put("/api/phieu-giam-gia/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        clear();
        mvc.perform(get("/api/phieu-giam-gia/" + id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.form").value(1));
        Number disabledCount = (Number) entityManager.createNativeQuery(
                        "select count(*) from phieu_giam_gia_khach_hang where id_phieu_giam_gia=:id and trang_thai=0")
                .setParameter("id", id).getSingleResult();
        assertEquals(1, disabledCount.intValue());
        mvc.perform(delete("/api/phieu-giam-gia/" + id)).andExpect(status().isOk());
        clear();
        mvc.perform(get("/api/phieu-giam-gia/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(0));
        mvc.perform(put("/api/phieu-giam-gia/" + id + "/kich-hoat")).andExpect(status().isOk());
        clear();
        mvc.perform(get("/api/phieu-giam-gia/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(1));
    }

    @Test
    void unlimitedPersistsAcrossReloadAndInvalidInputIs400RatherThan500() throws Exception {
        Map<String, Object> request = voucherRequest();
        request.put("unlimited", true);
        request.put("quantity", null);
        long id;
        if (database.hasColumn("phieu_giam_gia", "hinh_thuc_phieu")) {
            String body = mvc.perform(post("/api/phieu-giam-gia").contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            id = mapper.readTree(body).get("data").get("id").asLong();
        } else {
            // Legacy database cannot persist form. Quantity semantics still persist and reload without an extra column.
            var fixture = new com.smashstep.datn.promotion.entity.PhieuGiamGia();
            fixture.setMaPhieuGiamGia(testCode); fixture.setTenPhieuGiamGia(testCode); fixture.setSoLuong(null); fixture.setTrangThai(1);
            entityManager.persist(fixture); entityManager.flush(); id = fixture.getId();
        }
        clear();
        mvc.perform(get("/api/phieu-giam-gia/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unlimited").value(true)).andExpect(jsonPath("$.data.quantity").isEmpty());
        request.put("startDate", "2097-02-30");
        mvc.perform(put("/api/phieu-giam-gia/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void campaignUsesExistingRealVariantAndPersistsEditStatusWithoutChangingProduct() throws Exception {
        List<Long> ids = entityManager.createQuery(
                        "select s.id from SanPhamChiTiet s where s.trangThai=1 and s.kichHoat=true and s.idSanPham.trangThai=1 order by s.id", Long.class)
                .setMaxResults(1).getResultList();
        assertFalse(ids.isEmpty(), "Cần ít nhất một biến thể hoạt động hiện có để kiểm thử tích hợp");
        long variantId = ids.get(0);
        Object stockBefore = entityManager.createNativeQuery("select so_luong from san_pham_chi_tiet where id=:id")
                .setParameter("id", variantId).getSingleResult();
        Map<String, Object> request = new LinkedHashMap<>(Map.of("name", testCode, "discountValue", 10,
                "startDate", "2097-02-01", "endDate", "2097-02-28", "productDetailIds", List.of(variantId)));
        String body = mvc.perform(post("/api/dot-giam-gia").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String code = mapper.readTree(body).get("data").get("code").asText();
        clear();
        mvc.perform(get("/api/dot-giam-gia/" + code)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productDetailIds[0]").value(variantId));
        request.put("discountValue", 20);
        request.put("name", testCode + " updated");
        mvc.perform(put("/api/dot-giam-gia/" + code).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        clear();
        mvc.perform(get("/api/dot-giam-gia/" + code)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.discountValue").value(20));
        mvc.perform(patch("/api/dot-giam-gia/" + code + "/trang-thai").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":0}"))
                .andExpect(status().isOk());
        clear();
        mvc.perform(get("/api/dot-giam-gia/" + code)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(0));
        mvc.perform(patch("/api/dot-giam-gia/" + code + "/trang-thai").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":1}"))
                .andExpect(status().isOk());
        clear();
        mvc.perform(get("/api/dot-giam-gia").param("ma", testCode).param("trangThai", "1").param("page", "1").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
        Object stockAfter = entityManager.createNativeQuery("select so_luong from san_pham_chi_tiet where id=:id")
                .setParameter("id", variantId).getSingleResult();
        assertEquals(stockBefore, stockAfter);
        mvc.perform(get("/api/dot-giam-gia/capabilities")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.descriptionSupported").value(campaigns.supportsDescription()));
        if (!campaigns.supportsDescription()) {
            request.put("description", "Không được âm thầm mất mô tả");
            mvc.perform(put("/api/dot-giam-gia/" + code).contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
