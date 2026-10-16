package com.smashstep.datn.sales;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.smashstep.datn.DatnApplication;
import com.smashstep.datn.invoice.dto.CapNhatTrangThaiHoaDonDto;
import com.smashstep.datn.invoice.service.HoaDonService;
import com.smashstep.datn.sales.dto.SalesRequest;
import com.smashstep.datn.sales.service.SalesService;

import jakarta.persistence.EntityManager;

import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@SpringBootTest(
        classes = DatnApplication.class,
        properties = {
                "spring.jpa.show-sql=false",
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.jpa.open-in-view=false",
                "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.smashstep.datn.sales.CanonicalPosTichHopTest$SqlCapture"
        })
@Transactional
public class CanonicalPosTichHopTest {
    public static class SqlCapture implements StatementInspector {
        static final List<String> sql = new CopyOnWriteArrayList<>();

        public String inspect(String statement) {
            sql.add(statement.toLowerCase());
            return statement;
        }
    }

    @Autowired EntityManager em;
    @Autowired SalesService sales;
    @Autowired HoaDonService invoices;
    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper mapper;
    MockMvc mvc;

    @BeforeEach
    void resetCapture() {
        SqlCapture.sql.clear();
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void checkoutRetryAndStatusWorkflowUseCanonicalColumnsOnly(boolean selectedCustomer)
            throws Exception {
        var variant =
                sales.danhSachSanPhamBan(0, 100, "").getContent().stream()
                        .filter(v -> v.getTonKho() > 1)
                        .findFirst()
                        .orElseThrow();
        var request = new SalesRequest();
        request.setMaYeuCau(UUID.randomUUID().toString());
        request.setIdNhanVien(
                em.createQuery(
                                "select n.id from NhanVien n where n.trangThai=1 order by n.id",
                                Long.class)
                        .setMaxResults(1)
                        .getSingleResult());
        request.setIdPhuongThucThanhToan(sales.danhSachPhuongThucThanhToan().get(0).id());
        request.setTienKhachDua(new BigDecimal("100000000"));
        if (selectedCustomer)
            request.setIdKhachHang(
                    em.createQuery(
                                    "select k.id from KhachHang k where k.trangThai=1 order by"
                                            + " k.id",
                                    Long.class)
                            .setMaxResults(1)
                            .getSingleResult());
        var item = new SalesRequest.Item();
        item.setIdSanPhamChiTiet(variant.getId());
        item.setSoLuong(1);
        item.setDonGia(variant.getGiaBan());
        request.setDanhSachSanPham(List.of(item));
        long countBefore =
                em.createQuery("select count(h) from HoaDon h", Long.class).getSingleResult();
        var thanhToan =
                mvc.perform(
                                post("/api/ban-hang/thanh-toan")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(mapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andReturn();
        String code =
                mapper.readTree(thanhToan.getResponse().getContentAsString())
                        .path("data")
                        .path("invoiceCode")
                        .asText();
        var chuyenKetQuaThanhToan = sales.thanhToan(request);
        assertEquals(code, chuyenKetQuaThanhToan.getMaHoaDon());
        em.flush();
        em.clear();
        assertTrue(chuyenKetQuaThanhToan.getMaHoaDon().matches("HD[0-9]{6,}"));
        var detail = invoices.chiTiet(chuyenKetQuaThanhToan.getMaHoaDon());
        assertEquals(selectedCustomer ? 1 : 0, detail.getMaHinhThucNhan());
        assertEquals(selectedCustomer ? "Giao hàng" : "Nhận tại quầy", detail.getHinhThucNhan());
        assertEquals(0, detail.getMaLoaiHoaDon());
        assertEquals(selectedCustomer ? 0 : 8, detail.getMaTrangThai());
        mvc.perform(
                        post("/api/ban-hang/thanh-toan")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.invoiceCode").value(code));
        mvc.perform(get("/api/hoa-don/" + code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.maHinhThucNhan").value(selectedCustomer ? 1 : 0))
                .andExpect(
                        jsonPath("$.data.hinhThucNhan")
                                .value(selectedCustomer ? "Giao hàng" : "Nhận tại quầy"));
        assertEquals(
                countBefore + 1,
                em.createQuery("select count(h) from HoaDon h", Long.class).getSingleResult());
        assertEquals(variant.getTonKho() - 1, sales.chiTietSanPhamBan(variant.getId()).getTonKho());
        assertFalse(detail.getLichSuThanhToan().isEmpty());
        assertNotNull(detail.getIdNhanVien());
        for (int status : selectedCustomer ? new int[] {1, 2, 3, 4, 5} : new int[] {5}) {
            var update = new CapNhatTrangThaiHoaDonDto();
            update.setTrangThai(status);
            update.setGhiChu("Canonical compatibility test");
            mvc.perform(
                            put("/api/hoa-don/" + code + "/trang-thai")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(mapper.writeValueAsString(update)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.maTrangThai").value(status));
        }
        em.flush();
        em.clear();
        assertEquals(
                selectedCustomer ? 1 : 0,
                invoices.chiTiet(chuyenKetQuaThanhToan.getMaHoaDon()).getMaHinhThucNhan());
        assertTrue(SqlCapture.sql.stream().anyMatch(s -> s.startsWith("insert into hoa_don ")));
        assertTrue(
                SqlCapture.sql.stream()
                        .anyMatch(s -> s.startsWith("select") && s.contains("hoa_don")));
        assertFalse(
                SqlCapture.sql.stream()
                        .anyMatch(s -> s.contains("hinh_thuc_nhan") || s.contains("vo_han")));
        String sqlOutput = System.getProperty("smashstep.audit.sqlOutput");
        if (sqlOutput != null)
            java.nio.file.Files.writeString(
                    java.nio.file.Path.of(sqlOutput + "-" + selectedCustomer + ".sql"),
                    String.join(
                            ";\n",
                            SqlCapture.sql.stream().filter(s -> s.contains("hoa_don")).toList()));
    }
}
