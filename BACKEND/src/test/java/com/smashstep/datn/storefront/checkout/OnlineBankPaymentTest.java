package com.smashstep.datn.storefront.checkout;

import com.smashstep.datn.invoice.entity.HoaDon;
import com.smashstep.datn.invoice.entity.LichSuThanhToan;
import com.smashstep.datn.invoice.service.InvoiceCodeService;
import com.smashstep.datn.payment.entity.PhuongThucThanhToan;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OnlineBankPaymentTest {
    private EntityManager em;
    private HoaDon order;
    private TypedQuery<LichSuThanhToan> receipts;
    private static final Instant CREATED = Instant.parse("2026-10-20T03:00:00Z");
    private static final ZoneId ZONE = ZoneId.of("Asia/Bangkok");
    @BeforeEach @SuppressWarnings("unchecked") void setup() {
        em=mock(EntityManager.class);
        var marker=(TypedQuery<Long>)mock(TypedQuery.class,RETURNS_SELF);
        when(em.createQuery(anyString(),eq(Long.class))).thenReturn(marker); when(marker.getSingleResult()).thenReturn(1L);
        receipts=(TypedQuery<LichSuThanhToan>)mock(TypedQuery.class,RETURNS_SELF);
        when(em.createQuery(anyString(),eq(LichSuThanhToan.class))).thenReturn(receipts); when(receipts.getResultList()).thenReturn(List.of());
        var method=new PhuongThucThanhToan(); method.setMaPhuongThuc("CHUYEN_KHOAN");
        order=new HoaDon(); order.setId(1L); order.setMaHoaDon("HD000001"); order.setLoaiHoaDon(1); order.setTrangThai(0);
        order.setIdPhuongThucThanhToan(method); order.setNgayTao(LocalDateTime.ofInstant(CREATED,ZONE)); order.setThanhTien(new BigDecimal("999000.00"));
    }
    private OnlineBankPayment at(Instant instant) {
        return new OnlineBankPayment(em,mock(InvoiceCodeService.class),true,"970422","0966026145","  Vũ Chí   Tuấn Anh  ","MB Bank",Clock.fixed(instant,ZONE));
    }
    @Test void nameUsesUppercaseUnaccentedLettersAndSingleSpaces() throws Exception {
        assertEquals("VU CHI TUAN ANH",OnlineBankPayment.normalizeAccountName("  Vũ Chí   Tuấn Anh  "));
        assertEquals("DANG THI ANH",OnlineBankPayment.normalizeAccountName("Đặng\tThị Anh"));
        var p=at(CREATED).details(order); assertEquals("VU CHI TUAN ANH",p.accountName());
        assertTrue(p.qrUrl().contains("accountName=VU%20CHI%20TUAN%20ANH"));
        var properties=new java.util.Properties();
        try(var in=getClass().getResourceAsStream("/application.properties")) { assertNotNull(in); properties.load(in); }
        assertEquals("${PAYMENT_QR_ACCOUNT_NAME:VU CHI TUAN ANH}",properties.getProperty("app.payment.qr.account-name"));
    }
    @Test void fiveMinuteDeadlineComesFromPersistedCreationTime() {
        var p=at(CREATED.plusSeconds(299)).details(order);
        assertEquals("PENDING",p.status()); assertTrue(p.payable()); assertNotNull(p.qrUrl());
        assertEquals(CREATED.plusSeconds(300),p.expiresAt()); assertEquals(CREATED.plusSeconds(299),p.serverTime());
        assertEquals("999000",p.amount().toPlainString());
    }
    @Test void qrDisappearsAtExactDeadlineAndNeverReturnsOnReload() {
        for(long seconds:new long[]{300,301,3600}) {
            var p=at(CREATED.plusSeconds(seconds)).details(order);
            assertEquals("EXPIRED",p.status()); assertFalse(p.payable()); assertNull(p.qrUrl()); assertNull(p.accountNumber());
            assertEquals(CREATED.plusSeconds(300),p.expiresAt());
        }
    }
    @Test void alreadyPaidOrderStaysPaidAfterFiveMinutes() {
        var receipt=new LichSuThanhToan(); receipt.setTrangThai(1); receipt.setSoTien(order.getThanhTien());
        when(receipts.getResultList()).thenReturn(List.of(receipt)); order.setNgayThanhToan(order.getNgayTao().plusMinutes(1));
        var p=at(CREATED.plusSeconds(3600)).details(order);
        assertEquals("PAID",p.status()); assertFalse(p.payable()); assertNull(p.qrUrl());
    }
    @Test void closedAndRefundedOrdersNeverReturnQr() {
        order.setTrangThai(6); assertEquals("CLOSED",at(CREATED).details(order).status());
        var receipt=new LichSuThanhToan(); receipt.setTrangThai(2); when(receipts.getResultList()).thenReturn(List.of(receipt));
        assertEquals("REFUNDED",at(CREATED).details(order).status()); assertNull(at(CREATED).details(order).qrUrl());
    }
    @Test void missingCreationTimestampCannotCreateAnUnlimitedQr() {
        order.setNgayTao(null); var p=at(CREATED).details(order); assertEquals("UNAVAILABLE",p.status()); assertFalse(p.payable());
    }
    @Test void codHasNoQrOrExpiryAndInvalidVndAmountIsRejected() {
        order.getIdPhuongThucThanhToan().setMaPhuongThuc("COD"); var p=at(CREATED).details(order);
        assertEquals("OTHER",p.status()); assertNull(p.expiresAt()); assertNull(p.qrUrl());
        assertFalse(OnlineBankPayment.validAmount(new BigDecimal("1.5"))); assertFalse(OnlineBankPayment.validAmount(BigDecimal.ZERO));
    }
}
