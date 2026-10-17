package com.smashstep.datn.sales;

import com.smashstep.datn.DatnApplication;
import com.smashstep.datn.common.pricing.CurrentPriceService;
import com.smashstep.datn.customer.entity.KhachHang;
import com.smashstep.datn.employee.entity.NhanVien;
import com.smashstep.datn.invoice.entity.*;
import com.smashstep.datn.invoice.service.HoaDonService;
import com.smashstep.datn.product.entity.*;
import com.smashstep.datn.product.service.*;
import com.smashstep.datn.promotion.entity.*;
import com.smashstep.datn.sales.dto.SalesRequest;
import com.smashstep.datn.sales.service.SalesService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes=DatnApplication.class, properties={"spring.jpa.show-sql=false","spring.jpa.open-in-view=false"})
@Transactional
class PosPriceSynchronizationTest {
    @Autowired EntityManager em;
    @Autowired SalesService sales;
    @Autowired CurrentPriceService prices;
    @Autowired SanPhamService products;
    @Autowired SanPhamChiTietService variants;
    @Autowired HoaDonService invoices;
    private SanPham product; private SanPhamChiTiet a,b; private DotGiamGia campaign; private ChiTietDotGiamGia detail;
    private KhachHang customer; private NhanVien employee; private SalesRequest request;
    @BeforeEach void setup() {
        com.smashstep.datn.support.EmployeeSecurityFixture.authenticate(em);
        product=new SanPham(); product.setMaSanPham("TEST"+UUID.randomUUID()); product.setTenSanPham("Price synchronization"); product.setTrangThai(1); em.persist(product);
        a=variant("2000000"); b=variant("3000000");
        campaign=new DotGiamGia(); campaign.setMaDotGiamGia("TEST"+UUID.randomUUID()); campaign.setTenDotGiamGia("Sale test");
        campaign.setPhanTramGiamDot(new BigDecimal("10")); campaign.setTrangThai(1); campaign.setKichHoat(true);
        campaign.setNgayBatDau(LocalDateTime.now().minusDays(1)); campaign.setNgayKetThuc(LocalDateTime.now().plusDays(1)); em.persist(campaign);
        detail=new ChiTietDotGiamGia(); detail.setIdDotGiamGia(campaign); detail.setIdSanPhamChiTiet(a); detail.setTrangThai(1); em.persist(detail);
        employee=em.createQuery("select n from NhanVien n where n.trangThai=1 order by n.id desc",NhanVien.class).setMaxResults(1).getSingleResult();
        customer=new KhachHang(); customer.setMaKhachHang("TEST"+UUID.randomUUID()); customer.setTenKhachHang("Khách snapshot"); customer.setSoDienThoai("0911000007"); customer.setTrangThai(1); em.persist(customer);
        request=new SalesRequest(); request.setMaYeuCau(UUID.randomUUID().toString()); request.setIdKhachHang(customer.getId()); request.setIdNhanVien(employee.getId());
        request.setIdPhuongThucThanhToan(sales.danhSachPhuongThucThanhToan().get(0).id()); request.setTienKhachDua(new BigDecimal("4000000"));
        var item=new SalesRequest.Item(); item.setIdSanPhamChiTiet(a.getId()); item.setSoLuong(1); item.setDonGia(new BigDecimal("1800000")); request.setDanhSachSanPham(List.of(item)); em.flush();
    }
    @AfterEach void clearAuthentication() { com.smashstep.datn.support.EmployeeSecurityFixture.clear(); }
    private SanPhamChiTiet variant(String base) {
        var v=new SanPhamChiTiet(); v.setIdSanPham(product);
        v.setIdMauSac(em.createQuery("select m from MauSac m order by m.id",MauSac.class).setMaxResults(1).getSingleResult());
        int offset=base.equals("2000000") ? 0 : 1;
        v.setIdKichThuoc(em.createQuery("select k from KichThuoc k order by k.id",KichThuoc.class).setFirstResult(offset).setMaxResults(1).getSingleResult());
        v.setMaChiTietSanPham("TEST"+UUID.randomUUID()); v.setSku(v.getMaChiTietSanPham()); v.setGiaBan(new BigDecimal(base)); v.setSoLuong(10); v.setTrangThai(1); v.setKichHoat(true); em.persist(v); return v;
    }
    private void amount(String expected, BigDecimal actual) { assertEquals(0,new BigDecimal(expected).compareTo(actual)); }
    @Test void productVariantPosRangeAndHistoricalInvoiceUseSameServerPrice() {
        var p=products.layChiTietSanPham(product.getId()).getProduct();
        amount("2000000",p.getGiaThapNhat()); amount("3000000",p.getGiaCaoNhat()); amount("1800000",p.getGiaSauGiamThapNhat()); amount("3000000",p.getGiaSauGiamCaoNhat());
        amount("1800000",variants.layChiTiet(a.getId()).getGiaSauGiam()); amount("1800000",sales.chiTietSanPhamBan(a.getId()).getGiaBan());
        var receipt=sales.thanhToan(request); assertTrue(receipt.getMaHoaDon().matches("^HD[0-9]{6,}$"));
        var invoice=invoices.chiTiet(receipt.getMaHoaDon()); assertEquals(employee.getId(),invoice.getIdNhanVien()); assertEquals(employee.getMaNhanVien(),receipt.getMaNhanVien());
        assertEquals(employee.getTenNhanVien(),receipt.getTenNhanVien()); assertEquals(customer.getId(),invoice.getIdKhachHang()); assertEquals(customer.getMaKhachHang(),receipt.getMaKhachHang());
        assertEquals(receipt.getTenKhachHang(),invoice.getTenKhachHang()); assertEquals(receipt.getSoDienThoaiKhachHang(),invoice.getSoDienThoai());
        assertEquals(0,invoice.getMaLoaiHoaDon()); assertEquals(0,invoice.getMaTrangThai()); assertEquals(receipt.getTenPhuongThucThanhToan(),invoice.getPhuongThucThanhToan());
        assertEquals(request.getIdPhuongThucThanhToan(),invoice.getIdPhuongThucThanhToan()); assertEquals(receipt.getMaPhuongThucThanhToan(),invoice.getMaPhuongThucThanhToan());
        amount("1800000",invoice.getChiTietHoaDon().get(0).getDonGia()); amount("1800000",invoice.getTongTien());
        assertEquals("POS-REQ-"+request.getMaYeuCau(),invoice.getLichSuThanhToan().get(0).getMaGiaoDich());
        customer.setTenKhachHang("Changed profile"); customer.setSoDienThoai("0999999999"); campaign.setKichHoat(false); em.flush();
        invoice=invoices.chiTiet(receipt.getMaHoaDon()); assertEquals("Khách snapshot",invoice.getTenKhachHang()); assertEquals("0911000007",invoice.getSoDienThoai());
        assertEquals("Khách snapshot",invoices.timKiem(receipt.getMaHoaDon(),null,null,null,null,1,10).getContent().get(0).getTenKhachHang());
        amount("1800000",invoice.getChiTietHoaDon().get(0).getDonGia()); amount("2000000",sales.chiTietSanPhamBan(a.getId()).getGiaBan());
        assertEquals(receipt.getMaHoaDon(),sales.thanhToan(request).getMaHoaDon());
        request.setMaYeuCau(UUID.randomUUID().toString()); request.getDanhSachSanPham().get(0).setDonGia(new BigDecimal("2000000"));
        assertNotEquals(receipt.getMaHoaDon(),sales.thanhToan(request).getMaHoaDon());
    }
    @Test void campaignTimingOverrideAndInactiveDetailAreRespected() {
        detail.setPhanTramGiamBienThe(new BigDecimal("12")); em.flush(); amount("1760000",prices.getPrice(a,LocalDateTime.now()).getEffectivePrice());
        detail.setTrangThai(0); em.flush(); amount("2000000",sales.chiTietSanPhamBan(a.getId()).getGiaBan()); detail.setTrangThai(1);
        campaign.setNgayBatDau(LocalDateTime.now().plusDays(1)); campaign.setNgayKetThuc(LocalDateTime.now().plusDays(2)); em.flush(); amount("2000000",sales.chiTietSanPhamBan(a.getId()).getGiaBan());
        campaign.setNgayBatDau(LocalDateTime.now().minusDays(2)); campaign.setNgayKetThuc(LocalDateTime.now().minusDays(1)); em.flush(); amount("2000000",sales.chiTietSanPhamBan(a.getId()).getGiaBan());
        campaign.setNgayKetThuc(LocalDateTime.now().plusDays(1)); campaign.setTrangThai(0); em.flush(); amount("2000000",sales.chiTietSanPhamBan(a.getId()).getGiaBan());
    }
    @Test void guestVoucherAfterCampaignAndRetryCountsRemainConsistent() {
        request.setIdKhachHang(null);
        var voucher=new PhieuGiamGia(); voucher.setMaPhieuGiamGia("TEST"+UUID.randomUUID()); voucher.setTenPhieuGiamGia("Voucher test"); voucher.setHinhThucPhieu(1); voucher.setLoaiGiamGia(2);
        voucher.setGiaTriGiam(new BigDecimal("50000")); voucher.setGiaTriToiThieu(BigDecimal.ZERO); voucher.setSoLuong(10); voucher.setSoLuongDaDung(0); voucher.setTrangThai(1);
        voucher.setNgayBatDau(LocalDateTime.now().minusDays(1)); voucher.setNgayKetThuc(LocalDateTime.now().plusDays(1)); em.persist(voucher); request.setMaPhieuGiamGia(voucher.getMaPhieuGiamGia());
        var receipt=sales.thanhToan(request); var invoice=invoices.chiTiet(receipt.getMaHoaDon());
        assertNull(invoice.getIdKhachHang()); assertNull(invoice.getMaKhachHang()); assertEquals("Khách lẻ",invoice.getTenKhachHang());
        assertEquals("Khách lẻ",invoices.timKiem(receipt.getMaHoaDon(),null,null,null,null,1,10).getContent().get(0).getTenKhachHang());
        amount("1800000",receipt.getTongTienHang()); amount("50000",receipt.getTienGiam()); amount("1750000",receipt.getThanhTien());
        assertEquals(voucher.getId(),invoice.getIdPhieuGiamGia()); assertEquals(receipt.getMaPhieuGiamGia(),invoice.getMaPhieuGiamGia());
        assertEquals(receipt.getMaHoaDon(),sales.thanhToan(request).getMaHoaDon()); assertEquals(9,a.getSoLuong()); assertEquals(1,voucher.getSoLuongDaDung());
        assertEquals(1,em.createQuery("select p from LichSuThanhToan p where p.idHoaDon.id=:id",LichSuThanhToan.class).setParameter("id",receipt.getIdHoaDon()).getResultList().size());
        request.setTienKhachDua(new BigDecimal("5000000")); assertThrows(com.smashstep.datn.common.exception.AppException.class,()->sales.thanhToan(request));
    }
}
