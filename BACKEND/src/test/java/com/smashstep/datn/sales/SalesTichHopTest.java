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
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        var variant = sales.catalog(0, 1, "").getContent().get(0);
        oldStock = variant.stock();
        Long employeeId = em.createQuery("select n.id from NhanVien n where n.trangThai = 1 order by n.id", Long.class)
                .setMaxResults(1).getSingleResult();
        request = new SalesRequest(); request.setRequestId(UUID.randomUUID().toString());
        request.setEmployeeId(employeeId); request.setPaymentMethodId(sales.paymentMethods().get(0).id());
        request.setPaidAmount(new BigDecimal("100000000"));
        var item = new SalesRequest.Item(); item.setVariantId(variant.id()); item.setQuantity(1); item.setUnitPrice(variant.price());
        request.setItems(List.of(item));
    }

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
            request.setVoucherCode(voucher.getMaPhieuGiamGia());
        }
        long invoiceBefore = count("HoaDon"), itemsBefore = count("HoaDonChiTiet"), paymentBefore = count("LichSuThanhToan"), historyBefore = count("LichSuHoaDon");
        var quote = sales.quote(request);
        assertEquals(request.getVoucherCode() == null ? BigDecimal.ZERO : new BigDecimal("1000"), quote.discount().stripTrailingZeros().setScale(0));
        String body = mvc.perform(post("/api/sales/checkout").contentType(MediaType.APPLICATION_JSON).content(json()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andReturn().getResponse().getContentAsString();
        String invoiceCode = JsonMapper.builder().build().readTree(body).get("data").get("invoiceCode").asText();
        em.flush(); em.clear();
        assertEquals(oldStock - 1, em.find(SanPhamChiTiet.class, request.getItems().get(0).getVariantId()).getSoLuong());
        assertEquals(request.getVoucherCode() == null ? 0 : 1, em.find(PhieuGiamGia.class, voucher.getId()).getSoLuongDaDung());
        HoaDon invoice = em.createQuery("select h from HoaDon h where h.maHoaDon = :code", HoaDon.class).setParameter("code", invoiceCode).getSingleResult();
        assertEquals(5, invoice.getTrangThai()); assertEquals(0, quote.total().compareTo(invoice.getThanhTien()));
        assertEquals(invoiceBefore + 1, count("HoaDon")); assertEquals(itemsBefore + 1, count("HoaDonChiTiet"));
        assertEquals(paymentBefore + 1, count("LichSuThanhToan")); assertEquals(historyBefore + 1, count("LichSuHoaDon"));
        mvc.perform(post("/api/sales/checkout").contentType(MediaType.APPLICATION_JSON).content(json()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.invoiceCode").value(invoiceCode));
        em.flush(); em.clear();
        assertEquals(invoiceBefore + 1, count("HoaDon"));
        assertEquals(oldStock - 1, em.find(SanPhamChiTiet.class, request.getItems().get(0).getVariantId()).getSoLuong());
        mvc.perform(get("/api/hoa-don/" + invoiceCode)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tienGiamGia").value(request.getVoucherCode() == null ? 0 : 1000));
        request.getItems().get(0).setQuantity(2);
        mvc.perform(post("/api/sales/checkout").contentType(MediaType.APPLICATION_JSON).content(json())).andExpect(status().isConflict());
        assertEquals(invoiceBefore + 1, count("HoaDon"));
    }

    @Test
    void forgedPriceAndInsufficientStockAreRejectedWithoutWrites() throws Exception {
        long invoiceBefore = count("HoaDon");
        request.getItems().get(0).setUnitPrice(new BigDecimal("0.01"));
        mvc.perform(post("/api/sales/checkout").contentType(MediaType.APPLICATION_JSON).content(json())).andExpect(status().isConflict());
        request.getItems().get(0).setQuantity(oldStock + 1);
        mvc.perform(post("/api/sales/checkout").contentType(MediaType.APPLICATION_JSON).content(json())).andExpect(status().isConflict());
        assertEquals(invoiceBefore, count("HoaDon"));
        assertEquals(oldStock, em.find(SanPhamChiTiet.class, request.getItems().get(0).getVariantId()).getSoLuong());
    }

    @Test
    void invalidCartAndInsufficientPaymentDoNotCreateInvoices() throws Exception {
        long invoiceBefore = count("HoaDon");
        request.setPaidAmount(null);
        mvc.perform(post("/api/sales/quote").contentType(MediaType.APPLICATION_JSON).content(json())).andExpect(status().isOk());
        mvc.perform(post("/api/sales/checkout").contentType(MediaType.APPLICATION_JSON).content(json())).andExpect(status().isBadRequest());
        request.setPaidAmount(BigDecimal.ZERO);
        mvc.perform(post("/api/sales/checkout").contentType(MediaType.APPLICATION_JSON).content(json())).andExpect(status().isBadRequest());
        request.setItems(List.of());
        mvc.perform(post("/api/sales/checkout").contentType(MediaType.APPLICATION_JSON).content(json())).andExpect(status().isBadRequest());
        assertEquals(invoiceBefore, count("HoaDon"));
    }

    @Test
    void catalogQuoteAndPagingUseRealSellableVariants() throws Exception {
        mvc.perform(get("/api/sales/catalog").param("size", "1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));
        mvc.perform(get("/api/sales/catalog").param("page", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/sales/payment-methods")).andExpect(status().isOk());
        mvc.perform(post("/api/sales/quote").contentType(MediaType.APPLICATION_JSON).content(json())).andExpect(status().isOk());
        for (var item : sales.catalog(0, 100, "").getContent()) {
            SanPhamChiTiet actual = em.find(SanPhamChiTiet.class, item.id());
            assertTrue(actual.getSoLuong() > 0); assertTrue(actual.getKichHoat()); assertEquals(1, actual.getTrangThai());
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void checkoutStartsItsOwnPhysicalTransactionBeforeTakingAppLock() throws Exception {
        // No surrounding test transaction: this matches a real HTTP request.
        // Rejected checkout rolls back its own service transaction and never inserts an invoice.
        long invoiceBefore = count("HoaDon");
        request.setPaidAmount(BigDecimal.ZERO);
        mvc.perform(post("/api/sales/checkout").contentType(MediaType.APPLICATION_JSON).content(json()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Số tiền thanh toán chưa đủ"));
        assertEquals(invoiceBefore, count("HoaDon"));
        assertEquals(oldStock, sales.catalogItem(request.getItems().get(0).getVariantId()).stock());
    }
}
