package com.smashstep.datn.sales;

import com.smashstep.datn.DatnApplication;
import com.smashstep.datn.employee.entity.NhanVien;
import com.smashstep.datn.invoice.entity.HoaDon;
import com.smashstep.datn.product.entity.SanPhamChiTiet;
import com.smashstep.datn.promotion.entity.PhieuGiamGia;
import com.smashstep.datn.sales.dto.SalesRequest;
import com.smashstep.datn.sales.service.SalesService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = DatnApplication.class, properties = {"spring.jpa.show-sql=false", "spring.jpa.open-in-view=false"})
@Transactional
class SalesTichHopTest {
    @Autowired EntityManager em;
    @Autowired SalesService sales;
    @Autowired WebApplicationContext context;
    private MockMvc mvc;
    private SalesRequest request;
    private int oldStock;

    @BeforeEach
    void setup() {
        com.smashstep.datn.support.EmployeeSecurityFixture.authenticate(em);
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        var variant = sales.danhSachSanPhamBan(0, 1, "").getContent().get(0);
        oldStock = variant.getTonKho();
        Long employeeId = em.createQuery("select n.id from NhanVien n where n.trangThai = 1 order by n.id", Long.class)
                .setMaxResults(1).getSingleResult();
        request = new SalesRequest(); request.setMaYeuCau(UUID.randomUUID().toString());
        request.setIdNhanVien(employeeId); request.setIdPhuongThucThanhToan(sales.danhSachPhuongThucThanhToan().get(0).id());
        request.setTienKhachDua(new BigDecimal("100000000"));
        var item = new SalesRequest.Item(); item.setIdSanPhamChiTiet(variant.getId()); item.setSoLuong(1); item.setDonGia(variant.getGiaBan());
        request.setDanhSachSanPham(List.of(item));
    }

    @org.junit.jupiter.api.AfterEach
    void clearAuthentication() { com.smashstep.datn.support.EmployeeSecurityFixture.clear(); }

    private String json() { return JsonMapper.builder().build().writeValueAsString(request); }
    private long count(String entity) { return em.createQuery("select count(x) from " + entity + " x", Long.class).getSingleResult(); }

    @Test
    void checkoutPersistsAllPartsConsumesVoucherAndIsIdempotent() throws Exception {
        PhieuGiamGia voucher = new PhieuGiamGia(); voucher.setMaPhieuGiamGia("POS-TEST-" + UUID.randomUUID());
        voucher.setTenPhieuGiamGia("Phiếu kiểm thử POS"); voucher.setLoaiGiamGia(2);
        voucher.setGiaTriGiam(new BigDecimal("1000")); voucher.setGiaTriToiThieu(BigDecimal.ZERO);
        voucher.setNgayBatDau(LocalDateTime.now().minusDays(1)); voucher.setNgayKetThuc(LocalDateTime.now().plusDays(1));
        voucher.setSoLuong(2); voucher.setSoLuongDaDung(0); voucher.setHinhThucPhieu(1); voucher.setTrangThai(1);
        em.persist(voucher); em.flush(); if (em.getMetamodel().entity(PhieuGiamGia.class).getAttributes().stream().anyMatch(a -> a.getName().equals("hinhThucPhieu"))) {
            request.setMaPhieuGiamGia(voucher.getMaPhieuGiamGia());
        }
        long invoiceBefore = count("HoaDon"), itemsBefore = count("HoaDonChiTiet"), paymentBefore = count("LichSuThanhToan"), historyBefore = count("LichSuHoaDon");
        var quote = sales.tinhTien(request);
        assertEquals(request.getMaPhieuGiamGia() == null ? BigDecimal.ZERO : new BigDecimal("1000"), quote.getTienGiam().stripTrailingZeros().setScale(0));
        String body = mvc.perform(post("/api/ban-hang/thanh-toan").contentType(MediaType.APPLICATION_JSON).content(json()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andReturn().getResponse().getContentAsString();
        String invoiceCode = JsonMapper.builder().build().readTree(body).get("data").get("maHoaDon").asText();
        em.flush(); em.clear();
        assertEquals(oldStock - 1, em.find(SanPhamChiTiet.class, request.getDanhSachSanPham().get(0).getIdSanPhamChiTiet()).getSoLuong());
        assertEquals(request.getMaPhieuGiamGia() == null ? 0 : 1, em.find(PhieuGiamGia.class, voucher.getId()).getSoLuongDaDung());
        HoaDon invoice = em.createQuery("select h from HoaDon h where h.maHoaDon = :code", HoaDon.class).setParameter("code", invoiceCode).getSingleResult();
        assertEquals(8, invoice.getTrangThai()); assertEquals(0, quote.getThanhTien().compareTo(invoice.getThanhTien()));
        assertEquals(invoiceBefore + 1, count("HoaDon")); assertEquals(itemsBefore + 1, count("HoaDonChiTiet"));
        assertEquals(paymentBefore + 1, count("LichSuThanhToan")); assertEquals(historyBefore + 1, count("LichSuHoaDon"));
        mvc.perform(post("/api/ban-hang/thanh-toan").contentType(MediaType.APPLICATION_JSON).content(json()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.maHoaDon").value(invoiceCode));
        em.flush(); em.clear();
        assertEquals(invoiceBefore + 1, count("HoaDon"));
        assertEquals(oldStock - 1, em.find(SanPhamChiTiet.class, request.getDanhSachSanPham().get(0).getIdSanPhamChiTiet()).getSoLuong());
        mvc.perform(get("/api/hoa-don/" + invoiceCode)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tienGiamGia").value(request.getMaPhieuGiamGia() == null ? 0 : 1000));
        request.getDanhSachSanPham().get(0).setSoLuong(2);
        mvc.perform(post("/api/ban-hang/thanh-toan").contentType(MediaType.APPLICATION_JSON).content(json())).andExpect(status().isConflict());
        assertEquals(invoiceBefore + 1, count("HoaDon"));
    }

    @Test
    void forgedPriceAndInsufficientStockAreRejectedWithoutWrites() throws Exception {
        long invoiceBefore = count("HoaDon");
        request.getDanhSachSanPham().get(0).setDonGia(new BigDecimal("0.01"));
        mvc.perform(post("/api/ban-hang/thanh-toan").contentType(MediaType.APPLICATION_JSON).content(json())).andExpect(status().isConflict());
        request.getDanhSachSanPham().get(0).setSoLuong(oldStock + 1);
        mvc.perform(post("/api/ban-hang/thanh-toan").contentType(MediaType.APPLICATION_JSON).content(json())).andExpect(status().isConflict());
        assertEquals(invoiceBefore, count("HoaDon"));
        assertEquals(oldStock, em.find(SanPhamChiTiet.class, request.getDanhSachSanPham().get(0).getIdSanPhamChiTiet()).getSoLuong());
    }

    @Test
    void invalidCartAndInsufficientPaymentDoNotCreateInvoices() throws Exception {
        long invoiceBefore = count("HoaDon");
        request.setTienKhachDua(null);
        mvc.perform(post("/api/ban-hang/tinh-tien").contentType(MediaType.APPLICATION_JSON).content(json())).andExpect(status().isOk());
        mvc.perform(post("/api/ban-hang/thanh-toan").contentType(MediaType.APPLICATION_JSON).content(json())).andExpect(status().isBadRequest());
        request.setTienKhachDua(BigDecimal.ZERO);
        mvc.perform(post("/api/ban-hang/thanh-toan").contentType(MediaType.APPLICATION_JSON).content(json())).andExpect(status().isBadRequest());
        request.setDanhSachSanPham(List.of());
        mvc.perform(post("/api/ban-hang/thanh-toan").contentType(MediaType.APPLICATION_JSON).content(json())).andExpect(status().isBadRequest());
        assertEquals(invoiceBefore, count("HoaDon"));
    }

    @Test
    void catalogQuoteAndPagingUseRealSellableVariants() throws Exception {
        mvc.perform(get("/api/ban-hang/san-pham").param("kichThuocTrang", "1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));
        mvc.perform(get("/api/ban-hang/san-pham").param("trang", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/ban-hang/phuong-thuc-thanh-toan")).andExpect(status().isOk());
        mvc.perform(post("/api/ban-hang/tinh-tien").contentType(MediaType.APPLICATION_JSON).content(json())).andExpect(status().isOk());
        for (var item : sales.danhSachSanPhamBan(0, 100, "").getContent()) {
            SanPhamChiTiet actual = em.find(SanPhamChiTiet.class, item.getId());
            assertTrue(actual.getSoLuong() > 0); assertTrue(actual.getKichHoat()); assertEquals(1, actual.getTrangThai());
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void checkoutStartsItsOwnPhysicalTransactionBeforeTakingAppLock() throws Exception {
        // No surrounding test transaction: this matches a real HTTP request.
        // Rejected checkout rolls back its own service transaction and never inserts an invoice.
        long invoiceBefore = count("HoaDon");
        request.setTienKhachDua(BigDecimal.ZERO);
        mvc.perform(post("/api/ban-hang/thanh-toan").contentType(MediaType.APPLICATION_JSON).content(json()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Số tiền thanh toán chưa đủ"));
        assertEquals(invoiceBefore, count("HoaDon"));
        assertEquals(oldStock, sales.chiTietSanPhamBan(request.getDanhSachSanPham().get(0).getIdSanPhamChiTiet()).getTonKho());
    }
}
