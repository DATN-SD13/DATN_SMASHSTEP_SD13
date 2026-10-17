package com.smashstep.datn.sales;

import com.smashstep.datn.DatnApplication;
import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.invoice.service.HoaDonService;
import com.smashstep.datn.sales.dto.*;
import com.smashstep.datn.sales.service.*;
import com.smashstep.datn.support.EmployeeSecurityFixture;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes=DatnApplication.class,properties={"spring.jpa.hibernate.ddl-auto=none","spring.jpa.show-sql=false","spring.jpa.open-in-view=false"})
@Transactional
class TeamPaymentHistoryIntegrationTest {
    @Autowired EntityManager em;
    @Autowired SalesService sales;
    @Autowired PendingSaleService drafts;
    @Autowired HoaDonService invoices;
    @BeforeEach void setup(){EmployeeSecurityFixture.authenticate(em);}
    @AfterEach void clear(){EmployeeSecurityFixture.clear();}
    @ParameterizedTest @ValueSource(strings={"TIEN_MAT","CHUYEN_KHOAN","UNCONFIRMED_BANK"})
    void supportedPaymentPreservesAmountMethodReferenceAndInvoiceRelationship(String mode){
        boolean bank = !"TIEN_MAT".equals(mode);
        boolean confirmed = !"UNCONFIRMED_BANK".equals(mode);
        var variant=sales.danhSachSanPhamBan(0,100,"").getContent().stream().filter(v->v.getTonKho()>2&&v.getGiaBan().compareTo(BigDecimal.ONE)>0).findFirst().orElseThrow();
        var draft=drafts.taoHoaDonCho();
        draft=drafts.capNhatHoaDonCho(draft.id(),new PendingSaleRequest(draft.phienBan(),null,"",List.of(new PendingSaleRequest.Item(variant.getId(),BigDecimal.ONE,variant.getGiaBan()))));
        String methodCode = bank ? "CHUYEN_KHOAN" : "TIEN_MAT";
        Long methodId=em.createQuery("select p.id from PhuongThucThanhToan p where p.maPhuongThuc=:code and p.trangThai=1",Long.class).setParameter("code",methodCode).setMaxResults(1).getSingleResult();
        var payment=new SalesRequest.Payment();payment.setIdPhuongThucThanhToan(methodId);payment.setSoTien(variant.getGiaBan());
        var line=new SalesRequest.Item();line.setIdSanPhamChiTiet(variant.getId());line.setSoLuong(1);line.setDonGia(variant.getGiaBan());
        var request=new SalesRequest();request.setPhienBanThanhToan(2);request.setMaYeuCau(UUID.randomUUID().toString());request.setIdHoaDon(draft.id());request.setPhienBan(draft.phienBan());request.setGhiChu("");request.setDanhSachSanPham(List.of(line));request.setDanhSachThanhToan(List.of(payment));request.setTienKhachDua(bank?BigDecimal.ZERO:variant.getGiaBan().add(new BigDecimal("10000")));request.setDaXacNhanChuyenKhoan(confirmed);
        if(!confirmed){assertEquals(HttpStatus.BAD_REQUEST,assertThrows(AppException.class,()->sales.thanhToan(request)).getStatus());assertEquals(0L,em.createQuery("select count(p) from LichSuThanhToan p where p.idHoaDon.id=:id",Long.class).setParameter("id",draft.id()).getSingleResult());return;}
        var receipt=sales.thanhToan(request);em.flush();em.clear();
        var detail=invoices.chiTiet(receipt.getMaHoaDon());assertEquals(1,detail.getLichSuThanhToan().size());assertEquals(methodId,detail.getIdPhuongThucThanhToan());assertEquals(0,(bank?BigDecimal.ZERO:new BigDecimal("10000")).compareTo(receipt.getTienThua()));
        assertEquals(0,variant.getGiaBan().compareTo(detail.getLichSuThanhToan().stream().map(p->p.getSoTien()).reduce(BigDecimal.ZERO,BigDecimal::add)));
        assertNotNull(detail.getLichSuThanhToan().get(0).getPhuongThucThanhToan());
        assertTrue(detail.getLichSuThanhToan().stream().allMatch(p->p.getMaGiaoDich().startsWith("POS-REQ-"+request.getMaYeuCau()+"-")));
        assertEquals(receipt.getMaHoaDon(),sales.thanhToan(request).getMaHoaDon());
        assertEquals(1L,em.createQuery("select count(p) from LichSuThanhToan p where p.idHoaDon.id=:id",Long.class).setParameter("id",draft.id()).getSingleResult());
    }
}
