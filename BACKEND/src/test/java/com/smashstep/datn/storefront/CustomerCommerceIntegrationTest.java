package com.smashstep.datn.storefront;

import com.smashstep.datn.DatnApplication;
import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.customer.entity.KhachHang;
import com.smashstep.datn.product.entity.SanPhamChiTiet;
import com.smashstep.datn.payment.entity.PhuongThucThanhToan;
import com.smashstep.datn.promotion.entity.*;
import com.smashstep.datn.storefront.checkout.*;
import com.smashstep.datn.storefront.service.StorefrontProductService;
import com.smashstep.datn.auth.security.JwtAuthFilter.AuthDetails;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static com.smashstep.datn.storefront.checkout.CommerceDtos.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Run against an explicitly selected audit database. All fixtures are rolled back; no carrier calls. */
@SpringBootTest(classes=DatnApplication.class,properties={"spring.jpa.hibernate.ddl-auto=none","spring.jpa.show-sql=false","spring.sql.init.mode=never"})
@Transactional
class CustomerCommerceIntegrationTest {
    @Autowired EntityManager em;
    @Autowired CustomerCommerceService shop;
    @Autowired StorefrontProductService catalogue;
    @Autowired WebApplicationContext context;
    @Autowired com.smashstep.datn.invoice.service.HoaDonService invoices;
    @MockitoBean GhnShippingService carrier;
    Long customerId, otherId, methodId;
    SanPhamChiTiet variant;
    @BeforeEach void setup() {
        String database=(String)em.createNativeQuery("select DB_NAME()").getSingleResult();
        assertTrue(database.contains("Audit"),"Commerce fixtures must never run against the live database");
        var customers=em.createQuery("select c from KhachHang c where c.trangThai=1 order by c.id",KhachHang.class).setMaxResults(2).getResultList();
        assertEquals(2,customers.size());customerId=customers.get(0).getId();otherId=customers.get(1).getId();
        variant=em.createQuery("select v from SanPhamChiTiet v where v.trangThai=1 and v.kichHoat=true and v.idSanPham.trangThai=1 and v.idMauSac is not null and v.idKichThuoc is not null order by v.id",SanPhamChiTiet.class).setMaxResults(1).getSingleResult();
        variant.setSoLuong(100);variant.setGiaBan(new BigDecimal("1000000"));
        var method=em.createQuery("select p from PhuongThucThanhToan p where p.maPhuongThuc='COD'",PhuongThucThanhToan.class).getSingleResult();
        method.setTrangThai(1);if(method.getIdHinhThucThanhToan()!=null)method.getIdHinhThucThanhToan().setTrangThai(1);methodId=method.getId();
        when(carrier.fee(any(),anyInt(),any())).thenReturn(new BigDecimal("30000.00"));
        em.flush();
    }
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    Request request(int qty,String voucher,BigDecimal total,String id) {
        return new Request(List.of(new Item(variant.getId(),qty)),voucher,methodId,
                new Receiver("Nguyễn Văn An","0901234567","customer@example.com","Tỉnh trong fixture","Quận trong fixture","Phường trong fixture","Số 10 đường thử nghiệm",1,1,"1"),"Giao giờ hành chính",id,total);
    }
    Request ready(int qty,String voucher) {var r=request(qty,voucher,null,UUID.randomUUID().toString());return request(qty,voucher,shop.quote(customerId,r).total(),r.requestId());}
    @Test void checkoutIsIdempotentAndOrderPricesRemainHistorical() {
        var r=ready(2,null);var q=shop.quote(customerId,r);var first=shop.checkout(customerId,r);
        assertEquals(first.code(),shop.checkout(customerId,r).code());
        assertEquals(98,variant.getSoLuong());assertEquals(1,first.items().size());assertEquals(q.lines().get(0).variant().giaSauGiam(),first.items().get(0).unitPrice());
        variant.setGiaBan(new BigDecimal("9999999"));em.flush();
        assertEquals(first.items().get(0).unitPrice(),shop.order(customerId,first.code()).items().get(0).unitPrice());
        assertNull(em.createQuery("select h.ngayThanhToan from HoaDon h where h.maHoaDon=:c",LocalDateTime.class).setParameter("c",first.code()).getSingleResult());
        assertEquals(0L,em.createQuery("select count(p) from LichSuThanhToan p where p.idHoaDon.maHoaDon=:c",Long.class).setParameter("c",first.code()).getSingleResult());
    }
    @Test void ownershipAndCustomerStatusCannotBeBypassed() {
        var o=shop.checkout(customerId,ready(1,null));
        assertEquals(404,assertThrows(AppException.class,()->shop.order(otherId,o.code())).getStatus().value());
        assertEquals(404,assertThrows(AppException.class,()->shop.cancel(otherId,o.code())).getStatus().value());
        var h=em.createQuery("select h from HoaDon h where h.maHoaDon=:c",com.smashstep.datn.invoice.entity.HoaDon.class).setParameter("c",o.code()).getSingleResult();h.setTrangThai(1);em.flush();
        assertFalse(shop.order(customerId,o.code()).editable());assertThrows(AppException.class,()->shop.cancel(customerId,o.code()));
    }
    @Test void cancelRestoresStockOnceAndShippingEditDoesNotRepriceItems() {
        var r=ready(1,null);var o=shop.checkout(customerId,r);variant.setGiaBan(new BigDecimal("2000000"));
        var updated=shop.editShipping(customerId,o.code(),new Shipping(r.receiver(),"Ghi chú mới"));
        assertEquals(o.items().get(0).unitPrice(),updated.items().get(0).unitPrice());
        assertEquals(6,shop.cancel(customerId,o.code()).status());assertEquals(100,variant.getSoLuong());
        assertThrows(AppException.class,()->shop.cancel(customerId,o.code()));assertEquals(100,variant.getSoLuong());
    }
    @Test void staleTotalsAndInsufficientStockAreRejectedBeforeInvoiceCreation() {
        assertThrows(AppException.class,()->shop.quote(customerId,request(101,null,null,UUID.randomUUID().toString())));
        var r=ready(1,null);assertThrows(AppException.class,()->shop.checkout(customerId,request(1,null,r.expectedTotal().add(BigDecimal.ONE),r.requestId())));
        assertEquals(100,variant.getSoLuong());
    }
    @Test void privateVoucherUsesSharedEligibilityAndIsConsumedOnceThenReleased() {
        var p=new PhieuGiamGia();p.setMaPhieuGiamGia("WEB"+UUID.randomUUID().toString().substring(0,8));p.setTenPhieuGiamGia("Fixture riêng");p.setHinhThucPhieu(2);p.setLoaiGiamGia(1);p.setGiaTriGiam(new BigDecimal("10"));p.setGiaTriToiThieu(BigDecimal.ZERO);p.setGiamToiDa(new BigDecimal("200000"));p.setNgayBatDau(LocalDateTime.now().minusDays(1));p.setNgayKetThuc(LocalDateTime.now().plusDays(1));p.setTrangThai(1);p.setSoLuong(1);p.setSoLuongDaDung(0);em.persist(p);
        var a=new PhieuGiamGiaKhachHang();a.setIdPhieuGiamGia(p);a.setIdKhachHang(em.find(KhachHang.class,customerId));a.setTrangThai(1);em.persist(a);em.flush();
        assertFalse(shop.vouchers(otherId,new BigDecimal("2000000")).stream().anyMatch(v->v.code().equals(p.getMaPhieuGiamGia())));
        assertThrows(AppException.class,()->shop.quote(otherId,request(1,p.getMaPhieuGiamGia(),null,UUID.randomUUID().toString())));
        var r=ready(1,p.getMaPhieuGiamGia());var o=shop.checkout(customerId,r);shop.checkout(customerId,r);assertEquals(1,p.getSoLuongDaDung());assertNotNull(a.getNgaySuDung());
        shop.cancel(customerId,o.code());assertEquals(0,p.getSoLuongDaDung());assertNull(a.getNgaySuDung());
    }
    @Test void customerApiRejectsStaffAndCannotTakeAnArbitraryCustomerId() throws Exception {
        var mvc=MockMvcBuilders.webAppContextSetup(context).build();
        var auth=new UsernamePasswordAuthenticationToken("fixture",null,List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));auth.setDetails(new AuthDetails(customerId,"CUSTOMER","KHACH_HANG"));SecurityContextHolder.getContext().setAuthentication(auth);
        mvc.perform(get("/api/customer/shop/orders").principal(auth).param("customerId",otherId.toString())).andExpect(status().isOk());
        mvc.perform(get("/api/ban-hang/hoa-don-cho").principal(auth)).andExpect(status().isForbidden());
        mvc.perform(get("/api/hoa-don").principal(auth)).andExpect(status().isForbidden());
        var staff=new UsernamePasswordAuthenticationToken("fixture",null,List.of(new SimpleGrantedAuthority("ROLE_STAFF")));staff.setDetails(new AuthDetails(customerId,"STAFF","NHAN_VIEN"));SecurityContextHolder.getContext().setAuthentication(staff);
        mvc.perform(get("/api/customer/shop/orders").principal(staff)).andExpect(status().isForbidden());
    }
    @Test void fractionalCartQuantityIsNotSilentlyRounded() {
        assertThrows(IllegalArgumentException.class,()->Item.from(variant.getId(),new BigDecimal("1.5")));
    }
    @Test void productBadgeUsesMaximumDiscountIndependentlyOfCheapestVariant() {
        var rows=em.createQuery("select v from SanPhamChiTiet v where v.idSanPham.id=:p and v.trangThai=1 and v.kichHoat=true and v.idMauSac is not null and v.idKichThuoc is not null order by v.id",SanPhamChiTiet.class).setParameter("p",variant.getIdSanPham().getId()).getResultList();
        assertTrue(rows.size()>=2,"Audit fixture needs a product with multiple variants");
        for(var v:rows) v.setGiaBan(new BigDecimal("3000000"));
        var existing=em.createQuery("select c from ChiTietDotGiamGia c where c.idSanPhamChiTiet.id in :ids",ChiTietDotGiamGia.class).setParameter("ids",rows.stream().map(SanPhamChiTiet::getId).toList()).getResultList();
        existing.forEach(c->c.setTrangThai(0));
        rows.get(0).setGiaBan(new BigDecimal("1000000"));rows.get(1).setGiaBan(new BigDecimal("2000000"));
        campaign(rows.get(0),"10");campaign(rows.get(1),"35");em.flush();
        var detail=catalogue.detail(variant.getIdSanPham().getId());
        assertEquals(0,new BigDecimal("900000").compareTo(detail.product().giaSauGiam()));
        assertEquals(0,new BigDecimal("10").compareTo(detail.product().phanTramGiam()));
        assertEquals(0,new BigDecimal("35").compareTo(detail.product().phanTramGiamCaoNhat()));
        assertEquals(0,new BigDecimal("35").compareTo(detail.variants().stream().filter(v->v.id().equals(rows.get(1).getId())).findFirst().orElseThrow().phanTramGiam()));
    }
    private void campaign(SanPhamChiTiet variant,String percent) {
        var d=new DotGiamGia();d.setMaDotGiamGia("WEB"+UUID.randomUUID().toString().substring(0,8));d.setTenDotGiamGia("Pricing fixture");d.setPhanTramGiamDot(new BigDecimal(percent));d.setNgayBatDau(LocalDateTime.now().minusDays(1));d.setNgayKetThuc(LocalDateTime.now().plusDays(1));d.setTrangThai(1);d.setKichHoat(true);d.setNgayTao(LocalDateTime.now());em.persist(d);
        var c=new ChiTietDotGiamGia();c.setIdDotGiamGia(d);c.setIdSanPhamChiTiet(variant);c.setTrangThai(1);c.setNgayTao(LocalDateTime.now());em.persist(c);
    }
    @Test void carrierFailureCannotCreateAnOrderWithAnInventedShippingFee() {
        when(carrier.fee(any(),anyInt(),any())).thenThrow(AppException.badRequest("Chưa cấu hình GHN"));
        var q=shop.quote(customerId,request(1,null,null,UUID.randomUUID().toString()));assertNull(q.shipping());assertNull(q.total());assertFalse(q.canCheckout());
    }
    @Test void adminReadsAndCompletesWebCodThroughExistingInvoiceFlow() {
        var o=shop.checkout(customerId,ready(1,null));
        com.smashstep.datn.support.EmployeeSecurityFixture.authenticate(em);
        assertEquals(0,invoices.chiTiet(o.code()).getMaTrangThai());
        for(int status=1;status<=5;status++) {var r=new com.smashstep.datn.invoice.dto.CapNhatTrangThaiHoaDonDto();r.setTrangThai(status);invoices.capNhatTrangThai(o.code(),r);}
        assertEquals(1L,em.createQuery("select count(p) from LichSuThanhToan p where p.idHoaDon.maHoaDon=:c and p.trangThai=1",Long.class).setParameter("c",o.code()).getSingleResult());
        assertNotNull(em.createQuery("select h.ngayThanhToan from HoaDon h where h.maHoaDon=:c",LocalDateTime.class).setParameter("c",o.code()).getSingleResult());
        assertEquals(5,shop.order(customerId,o.code()).status());
    }
    @Test void adminCancelRestoresStockThroughExistingInvoiceFlow() {
        var o=shop.checkout(customerId,ready(1,null));
        com.smashstep.datn.support.EmployeeSecurityFixture.authenticate(em);
        var r=new com.smashstep.datn.invoice.dto.CapNhatTrangThaiHoaDonDto();r.setTrangThai(6);invoices.capNhatTrangThai(o.code(),r);
        assertEquals(100,variant.getSoLuong());assertEquals(6,shop.order(customerId,o.code()).status());
    }
}
