package com.smashstep.datn.storefront.checkout;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import static com.smashstep.datn.storefront.checkout.CommerceDtos.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class VnpaySandboxPaymentTest {
    private static final String TEST_KEY="unit-test-only-vnpay-signing-secret-0001";
    private CustomerCommerceService commerce;
    private DemoCardPayment sessions;
    private VnpaySandboxPayment gateway;
    private Request request;
    private Instant created;
    @BeforeEach void setup() {
        created=Instant.now().minusSeconds(10); commerce=mock(CustomerCommerceService.class);
        var variant=new Variant(1L,2L,"Nike Air Force 1","AF1-40","Trắng","40",null,10,
                new BigDecimal("999000"),new BigDecimal("999000"),BigDecimal.ZERO,false,true);
        var quote=new Quote(List.of(new Line(variant,1,new BigDecimal("999000"))),new BigDecimal("999000"),BigDecimal.ZERO,
                new BigDecimal("31000"),new BigDecimal("1030000"),null,List.of(),true,"");
        when(commerce.demoQuote(anyLong(),any())).thenReturn(quote);
        sessions=new DemoCardPayment(commerce,true,Clock.fixed(created,ZoneOffset.UTC));
        gateway=new VnpaySandboxPayment(sessions,true,"TEST0001",TEST_KEY,"https://sandbox.vnpayment.vn/paymentv2/vpcpay.html","https://example.test/thanh-toan-vnpay/ket-qua");
        request=new Request(List.of(new Item(1L,1)),null,null,new Receiver("Nguyễn Văn An","0912345678",null,"Hà Nội","","Ba Đình","12 Kim Mã",1,null,"1"),"",UUID.randomUUID().toString(),new BigDecimal("1030000"));
    }
    @AfterEach void noRealFinancialWrites() { verify(commerce,never()).checkout(anyLong(),any()); }
    private Map<String,String[]> signed(String code,String amount) {
        var fields=new TreeMap<String,String>(); fields.put("vnp_TmnCode","TEST0001"); fields.put("vnp_TxnRef",code);
        fields.put("vnp_Amount",amount); fields.put("vnp_ResponseCode","00"); fields.put("vnp_TransactionStatus","00"); fields.put("vnp_TransactionNo","12345678");
        fields.put("vnp_PayDate",DateTimeFormatter.ofPattern("uuuuMMddHHmmss").format(created.plusSeconds(1).atZone(ZoneId.of("Asia/Ho_Chi_Minh"))));
        var raw=new HashMap<String,String[]>(); fields.forEach((k,v)->raw.put(k,new String[]{v}));
        raw.put("vnp_SecureHash",new String[]{VnpaySandboxPayment.hmac(TEST_KEY,VnpaySandboxPayment.encoded(fields))}); return raw;
    }
    @Test void createsSignedGatewayUrlWithoutExposingSecretAndRetainsOriginalDeadline() {
        var start=gateway.create(1L,request,"127.0.0.1"); assertTrue(start.code().matches("SB[A-F0-9]{32}"));
        assertTrue(start.paymentUrl().startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?"));
        assertTrue(start.paymentUrl().contains("vnp_Amount=103000000")); assertTrue(start.paymentUrl().contains("vnp_BankCode=NCB"));
        assertFalse(start.paymentUrl().contains(TEST_KEY)); assertEquals(start.paymentUrl(),gateway.create(1L,request,"127.0.0.1").paymentUrl());
        assertTrue(sessions.paidOrders().isEmpty());
    }
    @Test void browserReturnNeverConfirmsButValidIpnConfirmsExactlyOnce() {
        var start=gateway.create(1L,request,"127.0.0.1"); var callback=signed(start.code(),"103000000");
        assertFalse(gateway.returned(callback).ipnConfirmed()); assertTrue(sessions.paidOrders().isEmpty());
        assertEquals("00",gateway.ipn(callback).get("RspCode")); assertTrue(gateway.returned(callback).ipnConfirmed());
        assertEquals("PAID_SANDBOX",gateway.owned(1L,start.code()).status()); assertEquals(1,sessions.paidOrders().size());
        assertEquals("02",gateway.ipn(callback).get("RspCode")); assertEquals(1,sessions.paidOrders().size());
    }
    @Test void invalidSignatureTamperedAmountMerchantAndDuplicateParametersCannotConfirm() {
        var start=gateway.create(1L,request,"127.0.0.1"); var callback=signed(start.code(),"103000000");
        callback.put("vnp_Amount",new String[]{"100"}); assertEquals("97",gateway.ipn(callback).get("RspCode"));
        assertEquals("04",gateway.ipn(signed(start.code(),"100")).get("RspCode"));
        callback=signed(start.code(),"103000000"); callback.put("vnp_TmnCode",new String[]{"WRONG001"}); assertEquals("97",gateway.ipn(callback).get("RspCode"));
        callback=signed(start.code(),"103000000"); callback.put("vnp_Amount",new String[]{"103000000","103000000"}); assertEquals("97",gateway.ipn(callback).get("RspCode"));
        assertTrue(sessions.paidOrders().isEmpty());
    }
    @Test void wrongOwnerAndLocalCardOtpCannotCompleteExternalSession() {
        var start=gateway.create(1L,request,"127.0.0.1");
        assertThrows(com.smashstep.datn.common.exception.AppException.class,()->gateway.owned(2L,start.code()));
        sessions.card(1L,start.code(),new DemoCardPayment.Card("9704198526191432198","NGUYEN VAN A","07/15"));
        sessions.otp(1L,start.code(),new DemoCardPayment.Otp("any","123456"));
        assertEquals("AWAITING_GATEWAY",gateway.owned(1L,start.code()).status()); assertTrue(sessions.paidOrders().isEmpty());
    }
    @Test void unknownOrderAndCanceledSessionsDoNotPublishPaidOrder() {
        assertEquals("01",gateway.ipn(signed("SB"+"A".repeat(32),"103000000")).get("RspCode"));
        var start=gateway.create(1L,request,"127.0.0.1"); sessions.cancel(1L,start.code());
        assertEquals("02",gateway.ipn(signed(start.code(),"103000000")).get("RspCode")); assertTrue(sessions.paidOrders().isEmpty());
    }
    @Test void productionEndpointsAndInvalidVndAmountsAreRejected() {
        var wrong=new VnpaySandboxPayment(sessions,true,"TEST0001",TEST_KEY,"https://pay.vnpay.vn/vpcpay.html","https://example.test/thanh-toan-vnpay/ket-qua");
        assertFalse(wrong.config().enabled()); assertFalse(VnpaySandboxPayment.validAmount(new BigDecimal("1.5")));
        assertFalse(VnpaySandboxPayment.validAmount(new BigDecimal("10000000000")));
        assertTrue(VnpaySandboxPayment.validAmount(new BigDecimal("10000")));
    }
}
