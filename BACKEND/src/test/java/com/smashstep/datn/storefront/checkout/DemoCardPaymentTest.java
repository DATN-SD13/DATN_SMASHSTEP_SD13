package com.smashstep.datn.storefront.checkout;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.customer.entity.KhachHang;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static com.smashstep.datn.storefront.checkout.CommerceDtos.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class DemoCardPaymentTest {
    private CustomerCommerceService commerce;
    private DemoCardPayment demo;
    private MutableClock clock;
    private Request request;
    private final DemoCardPayment.Card card=new DemoCardPayment.Card("9704198526191432198","NGUYEN VAN A","07/15");
    @BeforeEach void setup() {
        clock=new MutableClock(); commerce=mock(CustomerCommerceService.class);
        var customer=new KhachHang(); customer.setId(1L); when(commerce.customer(anyLong())).thenReturn(customer);
        var variant=new Variant(10L,20L,"Nike Air Force 1","AF1-TRANG-40","Trắng","40",null,10,
                new BigDecimal("999000"),new BigDecimal("999000"),BigDecimal.ZERO,false,true);
        var quote=new Quote(List.of(new Line(variant,1,new BigDecimal("999000"))),new BigDecimal("999000"),
                BigDecimal.ZERO,new BigDecimal("31000"),new BigDecimal("1030000"),null,List.of(),true,"");
        when(commerce.demoQuote(anyLong(),any())).thenReturn(quote);
        request=new Request(List.of(new Item(10L,1)),null,null,
                new Receiver("Nguyễn Văn An","0912345678",null,"Hà Nội","","Phường Ba Đình","12 Kim Mã",1,null,"1"),
                "",UUID.randomUUID().toString(),new BigDecimal("1030000"),false,false);
        demo=new DemoCardPayment(commerce,true,clock);
    }
    @AfterEach void neverCreatesRealInvoice() {
        verify(commerce,never()).checkout(anyLong(),any());
        verify(commerce,never()).editShipping(anyLong(),anyString(),any());
    }
    @Test void onlySuccessfulOtpPublishesDemoAndRepeatedSubmissionIsIdempotent() {
        var created=demo.create(1L,request); assertTrue(demo.paidOrders().isEmpty());
        assertEquals("AWAITING_CARD",created.status());
        assertEquals(created.code(),demo.create(1L,request).code());
        assertEquals("AWAITING_CARD",demo.otp(1L,created.code(),new DemoCardPayment.Otp("wrong","123456")).status());
        var challenge=demo.card(1L,created.code(),card); assertEquals("AWAITING_OTP",challenge.status());
        assertTrue(demo.paidOrders().isEmpty());
        var paid=demo.otp(1L,created.code(),new DemoCardPayment.Otp(challenge.challenge(),"123456"));
        assertEquals("PAID_DEMO",paid.status()); assertNotNull(paid.paidAt()); assertEquals(1,demo.paidOrders().size());
        assertEquals(paid.reference(),demo.otp(1L,created.code(),new DemoCardPayment.Otp(challenge.challenge(),"123456")).reference());
        assertEquals("PAID_DEMO",demo.cancel(1L,created.code()).status());
    }
    @Test void differentCustomerCannotReadPayOrCancelAnotherCustomersSession() {
        var created=demo.create(1L,request);
        assertThrows(AppException.class,()->demo.get(2L,created.code()));
        assertThrows(AppException.class,()->demo.card(2L,created.code(),card));
        assertThrows(AppException.class,()->demo.cancel(2L,created.code()));
        assertTrue(demo.paidOrders().isEmpty());
    }
    @Test void exactlyFiveMinutesExpiresAndOtpCannotReviveSession() {
        var created=demo.create(1L,request); var challenge=demo.card(1L,created.code(),card);
        assertEquals(Duration.ofMinutes(5),Duration.between(created.createdAt(),created.expiresAt()));
        clock.now=clock.now.plusSeconds(300);
        assertEquals("EXPIRED",demo.get(1L,created.code()).status());
        assertEquals("EXPIRED",demo.otp(1L,created.code(),new DemoCardPayment.Otp(challenge.challenge(),"123456")).status());
        assertTrue(demo.paidOrders().isEmpty());
    }
    @Test void onlyPublicTestCardsWorkAndFiveBadOtpAttemptsFail() {
        var created=demo.create(1L,request);
        assertEquals("AWAITING_CARD",demo.card(1L,created.code(),new DemoCardPayment.Card("4111111111111111","NGUYEN VAN A","07/15")).status());
        var challenge=demo.card(1L,created.code(),card);
        for(int i=0;i<4;i++) demo.otp(1L,created.code(),new DemoCardPayment.Otp(challenge.challenge(),"000000"));
        assertEquals("FAILED",demo.get(1L,created.code()).status());
        assertEquals("FAILED",demo.otp(1L,created.code(),new DemoCardPayment.Otp(challenge.challenge(),"123456")).status());
        assertTrue(demo.paidOrders().isEmpty());
    }
    @Test void insufficientFundsAndCanceledSessionsNeverAppearInManagement() {
        var created=demo.create(1L,request);
        assertEquals("FAILED",demo.card(1L,created.code(),new DemoCardPayment.Card("9704195798459170488","NGUYEN VAN A","07/15")).status());
        request=new Request(request.items(),null,null,request.receiver(),"",UUID.randomUUID().toString(),request.expectedTotal());
        var second=demo.create(1L,request); assertEquals("CANCELED",demo.cancel(1L,second.code()).status());
        assertEquals("CANCELED",demo.card(1L,second.code(),card).status()); assertTrue(demo.paidOrders().isEmpty());
    }
    @Test void alteredAmountsAndReusedRequestWithOtherItemsAreRejected() {
        var altered=new Request(request.items(),null,null,request.receiver(),"",UUID.randomUUID().toString(),BigDecimal.ONE);
        assertThrows(AppException.class,()->demo.create(1L,altered));
        demo.create(1L,request);
        var reused=new Request(List.of(new Item(10L,2)),null,null,request.receiver(),"",request.requestId(),request.expectedTotal());
        assertThrows(AppException.class,()->demo.create(1L,reused));
    }
    @Test void simulationDisabledByDefaultHasNoOrdersOrPaymentActions() {
        var disabled=new DemoCardPayment(commerce,false,clock); assertFalse(disabled.config().enabled());
        assertThrows(AppException.class,()->disabled.create(1L,request)); assertTrue(disabled.paidOrders().isEmpty());
    }
    @Test void managementPolicyHidesUnpaidTransferButKeepsCodAndPaidTransfer() {
        var invoice=new com.smashstep.datn.invoice.entity.HoaDon(); invoice.setLoaiHoaDon(1);
        var method=new com.smashstep.datn.payment.entity.PhuongThucThanhToan(); method.setMaPhuongThuc("CHUYEN_KHOAN"); invoice.setIdPhuongThucThanhToan(method);
        assertTrue(com.smashstep.datn.invoice.rules.HoaDonHienThiRules.choThanhToanOnline(invoice));
        method.setMaPhuongThuc("COD"); assertFalse(com.smashstep.datn.invoice.rules.HoaDonHienThiRules.choThanhToanOnline(invoice));
        method.setMaPhuongThuc("CHUYEN_KHOAN"); invoice.setNgayThanhToan(LocalDateTime.now());
        assertFalse(com.smashstep.datn.invoice.rules.HoaDonHienThiRules.choThanhToanOnline(invoice));
    }
    private static class MutableClock extends Clock {
        Instant now=Instant.parse("2026-10-20T10:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }
}
