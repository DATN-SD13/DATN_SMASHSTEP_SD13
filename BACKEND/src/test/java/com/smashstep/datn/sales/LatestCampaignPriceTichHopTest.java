package com.smashstep.datn.sales;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.smashstep.datn.DatnApplication;
import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.invoice.service.*;
import com.smashstep.datn.product.entity.*;
import com.smashstep.datn.promotion.entity.*;
import com.smashstep.datn.promotion.service.GiaSauGiamService;
import com.smashstep.datn.sales.dto.SalesRequest;
import com.smashstep.datn.sales.service.SalesService;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@SpringBootTest(
        classes = DatnApplication.class,
        properties = {
                "spring.jpa.show-sql=false",
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.jpa.open-in-view=false"
        })
@Transactional
class LatestCampaignPriceTichHopTest {
    @Autowired EntityManager em;
    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper mapper;
    @Autowired SalesService sales;
    @Autowired HoaDonService invoices;
    @Autowired ThongKeHoaDonService statistics;
    @Autowired GiaSauGiamService legacyPrices;
    MockMvc mvc;
    SanPham product;
    SanPhamChiTiet a, b;
    final Map<String, Object> evidence = new LinkedHashMap<>();

    void soTien(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    SanPhamChiTiet variant(String price, int sizeOffset) {
        var v = new SanPhamChiTiet();
        v.setIdSanPham(product);
        v.setIdMauSac(
                em.createQuery(
                                "select m from MauSac m where m.trangThai=1 order by m.id",
                                MauSac.class)
                        .setMaxResults(1)
                        .getSingleResult());
        v.setIdKichThuoc(
                em.createQuery(
                                "select k from KichThuoc k where k.trangThai=1 order by k.id",
                                KichThuoc.class)
                        .setFirstResult(sizeOffset)
                        .setMaxResults(1)
                        .getSingleResult());
        v.setMaChiTietSanPham("LATESTD" + UUID.randomUUID());
        v.setSku(v.getMaChiTietSanPham());
        v.setGiaBan(new BigDecimal(price));
        v.setSoLuong(10);
        v.setTrangThai(1);
        v.setKichHoat(true);
        em.persist(v);
        return v;
    }

    String read(String path) throws Exception {
        return mvc.perform(get(path))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }

    void capture(String name) throws Exception {
        em.flush();
        var row = new LinkedHashMap<String, String>();
        row.put("product", read("/api/products/" + product.getId()));
        row.put("productList", read("/api/products?keyword=" + product.getMaSanPham()));
        row.put("variants", read("/api/product-details?keyword=" + product.getMaSanPham()));
        row.put("pos", read("/api/ban-hang/san-pham/" + a.getId()));
        evidence.put(name, row);
    }

    @Test
    void currentCampaignPriceAcrossProductPosCheckoutHistoricalInvoiceAndStatistics()
            throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        product = new SanPham();
        product.setMaSanPham("LATEST" + UUID.randomUUID());
        product.setTenSanPham("Latest integration pricing fixture");
        product.setTrangThai(1);
        em.persist(product);
        a = variant("1000000", 0);
        b = variant("2000000", 1);
        capture("none");
        var campaign = new DotGiamGia();
        campaign.setMaDotGiamGia("LATEST" + UUID.randomUUID());
        campaign.setTenDotGiamGia("Latest campaign fixture");
        campaign.setPhanTramGiamDot(BigDecimal.TEN);
        campaign.setTrangThai(1);
        campaign.setKichHoat(true);
        campaign.setNgayBatDau(LocalDateTime.now().minusDays(1));
        campaign.setNgayKetThuc(LocalDateTime.now().plusDays(1));
        em.persist(campaign);
        var detail = new ChiTietDotGiamGia();
        detail.setIdDotGiamGia(campaign);
        detail.setIdSanPhamChiTiet(a);
        detail.setTrangThai(1);
        em.persist(detail);
        capture("active");
        soTien("900000", sales.chiTietSanPhamBan(a.getId()).getGiaBan());
        soTien("2000000", sales.chiTietSanPhamBan(b.getId()).getGiaBan());
        soTien("900000", legacyPrices.tinhGiaSauGiam(a.getId(), new BigDecimal("1")));
        var request = new SalesRequest();
        request.setMaYeuCau(UUID.randomUUID().toString());
        request.setIdNhanVien(
                em.createQuery(
                                "select n.id from NhanVien n where n.trangThai=1 order by n.id",
                                Long.class)
                        .setMaxResults(1)
                        .getSingleResult());
        request.setIdPhuongThucThanhToan(sales.danhSachPhuongThucThanhToan().get(0).id());
        request.setTienKhachDua(new BigDecimal("1000000"));
        var item = new SalesRequest.Item();
        item.setIdSanPhamChiTiet(a.getId());
        item.setSoLuong(1);
        item.setDonGia(new BigDecimal("900000"));
        request.setDanhSachSanPham(List.of(item));
        detail.setPhanTramGiamBienThe(new BigDecimal("15"));
        capture("override");
        soTien("850000", sales.chiTietSanPhamBan(a.getId()).getGiaBan());
        assertEquals(
                HttpStatus.CONFLICT,
                assertThrows(AppException.class, () -> sales.thanhToan(request)).getStatus());
        detail.setPhanTramGiamBienThe(null);
        campaign.setNgayBatDau(LocalDateTime.now().plusDays(1));
        campaign.setNgayKetThuc(LocalDateTime.now().plusDays(2));
        capture("future");
        soTien("1000000", sales.chiTietSanPhamBan(a.getId()).getGiaBan());
        campaign.setNgayBatDau(LocalDateTime.now().minusDays(2));
        campaign.setNgayKetThuc(LocalDateTime.now().minusDays(1));
        capture("expired");
        soTien("1000000", sales.chiTietSanPhamBan(a.getId()).getGiaBan());
        campaign.setNgayKetThuc(LocalDateTime.now().plusDays(1));
        campaign.setKichHoat(false);
        capture("inactive");
        soTien("1000000", sales.chiTietSanPhamBan(a.getId()).getGiaBan());
        campaign.setKichHoat(true);
        campaign.setTrangThai(0);
        capture("inactiveStatus");
        soTien("1000000", sales.chiTietSanPhamBan(a.getId()).getGiaBan());
        campaign.setTrangThai(1);
        detail.setTrangThai(0);
        capture("inactiveDetail");
        soTien("1000000", sales.chiTietSanPhamBan(a.getId()).getGiaBan());
        detail.setTrangThai(1);
        em.flush();
        var voucher = new PhieuGiamGia();
        voucher.setMaPhieuGiamGia("LATEST" + UUID.randomUUID());
        voucher.setTenPhieuGiamGia("After campaign fixture");
        voucher.setHinhThucPhieu(1);
        voucher.setLoaiGiamGia(2);
        voucher.setGiaTriGiam(new BigDecimal("50000"));
        voucher.setGiaTriToiThieu(BigDecimal.ZERO);
        voucher.setSoLuong(null);
        voucher.setSoLuongDaDung(0);
        voucher.setTrangThai(1);
        voucher.setNgayBatDau(LocalDateTime.now().minusDays(1));
        voucher.setNgayKetThuc(LocalDateTime.now().plusDays(1));
        em.persist(voucher);
        em.flush();
        request.setMaYeuCau(UUID.randomUUID().toString());
        request.setMaPhieuGiamGia(voucher.getMaPhieuGiamGia());
        var thanhToan =
                mvc.perform(
                                post("/api/ban-hang/thanh-toan")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(mapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        var chuyenKetQuaThanhToan = sales.thanhToan(request);
        assertTrue(chuyenKetQuaThanhToan.getMaHoaDon().matches("HD[0-9]{6,}"));
        soTien("900000", chuyenKetQuaThanhToan.getTongTienHang());
        soTien("50000", chuyenKetQuaThanhToan.getTienGiam());
        soTien("850000", chuyenKetQuaThanhToan.getThanhTien());
        var invoice = invoices.chiTiet(chuyenKetQuaThanhToan.getMaHoaDon());
        soTien("900000", invoice.getChiTietHoaDon().get(0).getDonGia());
        assertEquals(request.getIdNhanVien(), invoice.getIdNhanVien());
        assertEquals(request.getIdPhuongThucThanhToan(), invoice.getIdPhuongThucThanhToan());
        assertEquals(1, invoice.getLichSuThanhToan().size());
        mvc.perform(
                        put("/api/hoa-don/" + chuyenKetQuaThanhToan.getMaHoaDon() + "/trang-thai")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"trangThai\":5,\"ghiChu\":\"Rollback pricing integration"
                                                + " fixture\"}"))
                .andExpect(status().isOk());
        campaign.setKichHoat(false);
        capture("afterSale");
        soTien("1000000", sales.chiTietSanPhamBan(a.getId()).getGiaBan());
        invoice = invoices.chiTiet(chuyenKetQuaThanhToan.getMaHoaDon());
        soTien("900000", invoice.getChiTietHoaDon().get(0).getDonGia());
        soTien("850000", invoice.getThanhTien());
        var stats = statistics.tongHop(null, null, chuyenKetQuaThanhToan.getMaHoaDon(), null);
        assertEquals(1, stats.tongHoaDon());
        soTien("850000", stats.doanhThuDaThanhToan());
        soTien("1000000", a.getGiaBan());
        soTien("2000000", b.getGiaBan());
        evidence.put("checkout", thanhToan);
        evidence.put("invoice", read("/api/hoa-don/" + chuyenKetQuaThanhToan.getMaHoaDon()));
        evidence.put("statistics", read("/api/thong-ke?ma=" + chuyenKetQuaThanhToan.getMaHoaDon()));
        evidence.put("transactionRollback", true);
        String path = System.getProperty("smashstep.integration.evidence");
        if (path != null)
            java.nio.file.Files.writeString(
                    java.nio.file.Path.of(path), mapper.writeValueAsString(evidence));
    }

    @Test
    void latestFlatCampaignApiRetainsDateOnlyUpdateCompatibility() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        product = new SanPham();
        product.setMaSanPham("LATEST" + UUID.randomUUID());
        product.setTenSanPham("Campaign update fixture");
        product.setTrangThai(1);
        em.persist(product);
        a = variant("1000000", 0);
        em.flush();
        var data = new LinkedHashMap<String, Object>();
        data.put("name", product.getTenSanPham());
        data.put("discountValue", 10);
        data.put("startDate", "2097-02-01");
        data.put("endDate", "2097-02-28");
        data.put("productDetailIds", List.of(a.getId()));
        var created =
                mvc.perform(
                                post("/api/dot-giam-gia")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(mapper.writeValueAsString(data)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String code = mapper.readTree(created).path("data").path("code").asText();
        em.flush();
        em.clear();
        data.put("discountValue", 20);
        data.put("name", "Updated campaign fixture");
        mvc.perform(
                        put("/api/dot-giam-gia/" + code)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(data)))
                .andExpect(status().isOk());
    }
}
