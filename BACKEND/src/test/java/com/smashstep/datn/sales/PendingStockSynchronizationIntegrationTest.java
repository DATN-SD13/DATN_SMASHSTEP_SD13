package com.smashstep.datn.sales;

import com.smashstep.datn.DatnApplication;
import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.invoice.dto.CapNhatTrangThaiHoaDonDto;
import com.smashstep.datn.invoice.entity.HoaDon;
import com.smashstep.datn.invoice.rules.PosDraftRules;
import com.smashstep.datn.invoice.service.HoaDonService;
import com.smashstep.datn.product.entity.*;
import com.smashstep.datn.sales.dto.*;
import com.smashstep.datn.sales.service.*;
import com.smashstep.datn.support.EmployeeSecurityFixture;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes=DatnApplication.class,properties={"spring.jpa.hibernate.ddl-auto=none","spring.jpa.show-sql=false","spring.jpa.open-in-view=false"})
@Transactional
class PendingStockSynchronizationIntegrationTest {
    @Autowired EntityManager em;
    @Autowired PendingSaleService drafts;
    @Autowired SalesService sales;
    @Autowired HoaDonService invoices;
    Long productId,variantId;
    @BeforeEach void setup(){
        EmployeeSecurityFixture.authenticate(em);
        var p=new SanPham();p.setMaSanPham("STOCK-"+UUID.randomUUID());p.setTenSanPham("Stock integration");p.setTrangThai(1);em.persist(p);
        var v=new SanPhamChiTiet();v.setIdSanPham(p);v.setMaChiTietSanPham("STOCK-"+UUID.randomUUID());v.setSku(v.getMaChiTietSanPham());
        v.setIdMauSac(em.createQuery("select m from MauSac m order by m.id",MauSac.class).setMaxResults(1).getSingleResult());
        v.setIdKichThuoc(em.createQuery("select k from KichThuoc k order by k.id",KichThuoc.class).setMaxResults(1).getSingleResult());
        v.setTrangThai(1);v.setKichHoat(true);v.setGiaBan(new BigDecimal("1000000.00"));v.setSoLuong(8);em.persist(v);em.flush();
        productId=p.getId();variantId=v.getId();
    }
    @AfterEach void clear(){EmployeeSecurityFixture.clear();}
    int stock(){em.flush();em.clear();return em.find(SanPhamChiTiet.class,variantId).getSoLuong();}
    PendingSaleService.Draft save(PendingSaleService.Draft draft,int qty){
        return drafts.capNhatHoaDonCho(draft.id(),new PendingSaleRequest(draft.phienBan(),null,"stock test",qty==0?List.of():List.of(new PendingSaleRequest.Item(variantId,BigDecimal.valueOf(qty),new BigDecimal("1000000.00")))));
    }
    @Test void stockDeltaTracksQuantityChangesAndRemovalExactlyOnce(){
        var draft=save(drafts.taoHoaDonCho(),2);assertEquals(6,stock());
        draft=save(draft,3);assertEquals(5,stock());
        draft=save(draft,1);assertEquals(7,stock());
        draft=save(draft,0);assertEquals(8,stock());assertTrue(draft.danhSachSanPham().isEmpty());
        assertTrue(PosDraftRules.daTruKho(em.find(HoaDon.class,draft.id())));
    }
    @Test void cancellationRestoresReservedStockAndRetryDoesNotRestoreTwice(){
        var draft=save(drafts.taoHoaDonCho(),3);assertEquals(5,stock());
        var request=new CapNhatTrangThaiHoaDonDto();request.setTrangThai(6);request.setNguonThaoTac("POS");request.setGhiChu("Cancel stock test");
        invoices.capNhatTrangThai(draft.maHoaDon(),request);assertEquals(8,stock());
        assertThrows(AppException.class,()->invoices.capNhatTrangThai(draft.maHoaDon(),request));
        assertEquals(8,stock());
    }
    @Test void deactivatingProductRemovesItsDraftLineAndRestoresStockIdempotently(){
        var draft=save(drafts.taoHoaDonCho(),2);assertEquals(6,stock());
        em.find(SanPham.class,productId).setTrangThai(0);em.flush();
        var synced=drafts.dongBoHoaDonCho().stream().filter(d->d.id().equals(draft.id())).findFirst().orElseThrow();
        assertTrue(synced.danhSachSanPham().isEmpty());assertEquals(0,synced.thanhTien().compareTo(BigDecimal.ZERO));assertEquals(8,stock());
        drafts.dongBoHoaDonCho();assertEquals(8,stock());
    }
    @Test void ownReservationCanUseAllStockAndCheckoutDoesNotDeductTwice(){
        var draft=save(drafts.taoHoaDonCho(),8);assertEquals(0,stock());
        var synced=drafts.dongBoHoaDonCho().stream().filter(d->d.id().equals(draft.id())).findFirst().orElseThrow();
        assertEquals(1,synced.danhSachSanPham().size());assertEquals(8,synced.danhSachSanPham().get(0).soLuong());assertEquals(0,stock());
        var line=new SalesRequest.Item();line.setIdSanPhamChiTiet(variantId);line.setSoLuong(8);line.setDonGia(new BigDecimal("1000000.00"));
        Long cash=sales.danhSachPhuongThucThanhToan().stream().filter(m->"TIEN_MAT".equals(m.maPhuongThucThanhToan())).findFirst().orElseThrow().id();
        var payment=new SalesRequest.Payment();payment.setIdPhuongThucThanhToan(cash);payment.setSoTien(new BigDecimal("8000000.00"));
        var request=new SalesRequest();request.setPhienBanThanhToan(2);request.setMaYeuCau(UUID.randomUUID().toString());request.setIdHoaDon(draft.id());request.setPhienBan(synced.phienBan());request.setGhiChu("stock test");request.setDanhSachSanPham(List.of(line));request.setDanhSachThanhToan(List.of(payment));request.setTienKhachDua(new BigDecimal("8000000.00"));
        var receipt=sales.thanhToan(request);assertEquals(0,stock());
        assertEquals(receipt.getMaHoaDon(),sales.thanhToan(request).getMaHoaDon());assertEquals(0,stock());
        assertEquals(5,invoices.chiTiet(receipt.getMaHoaDon()).getMaTrangThai());
    }
    @Test void otherCartCannotReserveStockAlreadyDeductedByFirstCart(){
        save(drafts.taoHoaDonCho(),7);assertEquals(1,stock());
        var other=drafts.taoHoaDonCho();assertThrows(AppException.class,()->save(other,2));assertEquals(1,stock());
    }
}
