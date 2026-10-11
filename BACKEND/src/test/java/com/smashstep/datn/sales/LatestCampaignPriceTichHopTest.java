package com.smashstep.datn.sales;

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
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=DatnApplication.class, properties={"spring.jpa.show-sql=false", "spring.jpa.hibernate.ddl-auto=none", "spring.jpa.open-in-view=false"})
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
    SanPhamChiTiet a,b;
    final Map<String,Object> evidence=new LinkedHashMap<>();

    void amount(String expected,BigDecimal actual){assertEquals(0,new BigDecimal(expected).compareTo(actual));}
    SanPhamChiTiet variant(String price,int sizeOffset){
        var v=new SanPhamChiTiet();v.setIdSanPham(product);
        v.setIdMauSac(em.createQuery("select m from MauSac m where m.trangThai=1 order by m.id",MauSac.class).setMaxResults(1).getSingleResult());
        v.setIdKichThuoc(em.createQuery("select k from KichThuoc k where k.trangThai=1 order by k.id",KichThuoc.class).setFirstResult(sizeOffset).setMaxResults(1).getSingleResult());
        v.setMaChiTietSanPham("LATESTD"+UUID.randomUUID());v.setSku(v.getMaChiTietSanPham());v.setGiaBan(new BigDecimal(price));v.setSoLuong(10);v.setTrangThai(1);v.setKichHoat(true);em.persist(v);return v;
    }
    String read(String path)throws Exception{return mvc.perform(get(path)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);}
    void capture(String name)throws Exception{
        em.flush();var row=new LinkedHashMap<String,String>();row.put("product",read("/api/products/"+product.getId()));
        row.put("productList",read("/api/products?keyword="+product.getMaSanPham()));
        row.put("variants",read("/api/product-details?keyword="+product.getMaSanPham()));
        row.put("pos",read("/api/sales/catalog/"+a.getId()));evidence.put(name,row);
    }
    @Test void currentCampaignPriceAcrossProductPosCheckoutHistoricalInvoiceAndStatistics()throws Exception{
        mvc=MockMvcBuilders.webAppContextSetup(context).build();
        product=new SanPham();product.setMaSanPham("LATEST"+UUID.randomUUID());product.setTenSanPham("Latest integration pricing fixture");product.setTrangThai(1);em.persist(product);
        a=variant("1000000",0);b=variant("2000000",1);capture("none");
        var campaign=new DotGiamGia();campaign.setMaDotGiamGia("LATEST"+UUID.randomUUID());campaign.setTenDotGiamGia("Latest campaign fixture");campaign.setPhanTramGiamDot(BigDecimal.TEN);campaign.setTrangThai(1);campaign.setKichHoat(true);
        campaign.setNgayBatDau(LocalDateTime.now().minusDays(1));campaign.setNgayKetThuc(LocalDateTime.now().plusDays(1));em.persist(campaign);
        var detail=new ChiTietDotGiamGia();detail.setIdDotGiamGia(campaign);detail.setIdSanPhamChiTiet(a);detail.setTrangThai(1);em.persist(detail);capture("active");
        amount("900000",sales.catalogItem(a.getId()).price());amount("2000000",sales.catalogItem(b.getId()).price());
        amount("900000",legacyPrices.tinhGiaSauGiam(a.getId(),new BigDecimal("1")));
        var request=new SalesRequest();request.setRequestId(UUID.randomUUID().toString());
        request.setEmployeeId(em.createQuery("select n.id from NhanVien n where n.trangThai=1 order by n.id",Long.class).setMaxResults(1).getSingleResult());request.setPaymentMethodId(sales.paymentMethods().get(0).id());request.setPaidAmount(new BigDecimal("1000000"));
        var item=new SalesRequest.Item();item.setVariantId(a.getId());item.setQuantity(1);item.setUnitPrice(new BigDecimal("900000"));request.setItems(List.of(item));
        detail.setPhanTramGiamBienThe(new BigDecimal("15"));capture("override");amount("850000",sales.catalogItem(a.getId()).price());
        assertEquals(HttpStatus.CONFLICT,assertThrows(AppException.class,()->sales.checkout(request)).getStatus());
        detail.setPhanTramGiamBienThe(null);
        campaign.setNgayBatDau(LocalDateTime.now().plusDays(1));campaign.setNgayKetThuc(LocalDateTime.now().plusDays(2));capture("future");amount("1000000",sales.catalogItem(a.getId()).price());
        campaign.setNgayBatDau(LocalDateTime.now().minusDays(2));campaign.setNgayKetThuc(LocalDateTime.now().minusDays(1));capture("expired");amount("1000000",sales.catalogItem(a.getId()).price());
        campaign.setNgayKetThuc(LocalDateTime.now().plusDays(1));campaign.setKichHoat(false);capture("inactive");amount("1000000",sales.catalogItem(a.getId()).price());
        campaign.setKichHoat(true);campaign.setTrangThai(0);capture("inactiveStatus");amount("1000000",sales.catalogItem(a.getId()).price());
        campaign.setTrangThai(1);detail.setTrangThai(0);capture("inactiveDetail");amount("1000000",sales.catalogItem(a.getId()).price());detail.setTrangThai(1);em.flush();
        var voucher=new PhieuGiamGia();voucher.setMaPhieuGiamGia("LATEST"+UUID.randomUUID());voucher.setTenPhieuGiamGia("After campaign fixture");voucher.setHinhThucPhieu(1);voucher.setLoaiGiamGia(2);voucher.setGiaTriGiam(new BigDecimal("50000"));voucher.setGiaTriToiThieu(BigDecimal.ZERO);voucher.setSoLuong(null);voucher.setSoLuongDaDung(0);voucher.setTrangThai(1);voucher.setNgayBatDau(LocalDateTime.now().minusDays(1));voucher.setNgayKetThuc(LocalDateTime.now().plusDays(1));em.persist(voucher);em.flush();
        request.setRequestId(UUID.randomUUID().toString());request.setVoucherCode(voucher.getMaPhieuGiamGia());
        var checkout=mvc.perform(post("/api/sales/checkout").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(request))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        var receipt=sales.checkout(request);assertTrue(receipt.invoiceCode().matches("HD[0-9]{6,}"));amount("900000",receipt.subtotal());amount("50000",receipt.discount());amount("850000",receipt.total());
        var invoice=invoices.chiTiet(receipt.invoiceCode());amount("900000",invoice.getChiTietHoaDon().get(0).getDonGia());assertEquals(request.getEmployeeId(),invoice.getIdNhanVien());assertEquals(request.getPaymentMethodId(),invoice.getIdPhuongThucThanhToan());assertEquals(1,invoice.getLichSuThanhToan().size());
        mvc.perform(put("/api/hoa-don/"+receipt.invoiceCode()+"/trang-thai").contentType(MediaType.APPLICATION_JSON).content("{\"trangThai\":5,\"ghiChu\":\"Rollback pricing integration fixture\"}")).andExpect(status().isOk());
        campaign.setKichHoat(false);capture("afterSale");amount("1000000",sales.catalogItem(a.getId()).price());
        invoice=invoices.chiTiet(receipt.invoiceCode());amount("900000",invoice.getChiTietHoaDon().get(0).getDonGia());amount("850000",invoice.getThanhTien());
        var stats=statistics.tongHop(null,null,receipt.invoiceCode(),null);assertEquals(1,stats.tongHoaDon());amount("850000",stats.doanhThuDaThanhToan());
        amount("1000000",a.getGiaBan());amount("2000000",b.getGiaBan());
        evidence.put("checkout",checkout);evidence.put("invoice",read("/api/hoa-don/"+receipt.invoiceCode()));evidence.put("statistics",read("/api/thong-ke?ma="+receipt.invoiceCode()));evidence.put("transactionRollback",true);
        String path=System.getProperty("smashstep.integration.evidence");if(path!=null)java.nio.file.Files.writeString(java.nio.file.Path.of(path),mapper.writeValueAsString(evidence));
    }
    @Test void latestFlatCampaignApiRetainsDateOnlyUpdateCompatibility() throws Exception {
        mvc=MockMvcBuilders.webAppContextSetup(context).build();
        product=new SanPham();product.setMaSanPham("LATEST"+UUID.randomUUID());product.setTenSanPham("Campaign update fixture");product.setTrangThai(1);em.persist(product);a=variant("1000000",0);em.flush();
        var data=new LinkedHashMap<String,Object>();data.put("name",product.getTenSanPham());data.put("discountValue",10);data.put("startDate","2097-02-01");data.put("endDate","2097-02-28");data.put("productDetailIds",List.of(a.getId()));
        var created=mvc.perform(post("/api/dot-giam-gia").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(data))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String code=mapper.readTree(created).path("data").path("code").asText();em.flush();em.clear();data.put("discountValue",20);data.put("name","Updated campaign fixture");
        mvc.perform(put("/api/dot-giam-gia/"+code).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(data)))
            .andExpect(status().isOk());
    }

}
