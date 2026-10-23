package com.smashstep.datn.storefront.checkout;

import com.smashstep.datn.common.exception.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** VNPAY Sandbox transport. Verified payments publish normal local invoices through VnpayOrderService. */
@Service
public class VnpaySandboxPayment {
    private static final String SANDBOX_URL="https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private static final ZoneId ZONE=ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("uuuuMMddHHmmss").withResolverStyle(java.time.format.ResolverStyle.STRICT);
    private final DemoCardPayment sessions;
    private final VnpayOrderService orders;
    private final boolean enabled,allowLocalReturn;
    private final String terminal,secret,paymentUrl,returnUrl;
    @Autowired
    public VnpaySandboxPayment(DemoCardPayment sessions,
            @Value("${app.payment.vnpay.enabled:false}") boolean enabled,
            @Value("${app.payment.vnpay.tmn-code:}") String terminal,
            @Value("${app.payment.vnpay.hash-secret:}") String secret,
            @Value("${app.payment.vnpay.payment-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}") String paymentUrl,
            @Value("${app.payment.vnpay.return-url:}") String returnUrl,
            @Value("${app.payment.vnpay.allow-local-return:false}") boolean allowLocalReturn, VnpayOrderService orders) {
        this.sessions=sessions; this.orders=orders; this.enabled=enabled; this.allowLocalReturn=allowLocalReturn; this.terminal=terminal.trim(); this.secret=secret.trim();
        this.paymentUrl=paymentUrl.trim(); this.returnUrl=returnUrl.trim();
    }
    public VnpaySandboxPayment(DemoCardPayment sessions,boolean enabled,String terminal,String secret,String paymentUrl,String returnUrl) {
        this(sessions,enabled,terminal,secret,paymentUrl,returnUrl,false,null);
    }
    public VnpaySandboxPayment(DemoCardPayment sessions,boolean enabled,String terminal,String secret,String paymentUrl,String returnUrl,boolean allowLocalReturn) {
        this(sessions,enabled,terminal,secret,paymentUrl,returnUrl,allowLocalReturn,null);
    }
    public record Config(boolean enabled,String mode) {}
    public record Start(String code,String status,String paymentUrl) {}
    public record Result(boolean valid,String code,String status,String message,
            @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) BigDecimal amount,
            Instant paidAt,Instant expiresAt,Instant serverTime,boolean ipnConfirmed,boolean paymentConfirmed,String confirmationMethod,String orderCode,String orderMessage) {
        public Result(boolean valid,String code,String status,String message,BigDecimal amount,Instant paidAt,Instant expiresAt,
                Instant serverTime,boolean ipnConfirmed,boolean paymentConfirmed,String confirmationMethod) {
            this(valid,code,status,message,amount,paidAt,expiresAt,serverTime,ipnConfirmed,paymentConfirmed,confirmationMethod,null,null);
        }
        public Result(boolean valid,String code,String status,String message,BigDecimal amount,Instant paidAt,
                Instant expiresAt,Instant serverTime,boolean ipnConfirmed) {
            this(valid,code,status,message,amount,paidAt,expiresAt,serverTime,ipnConfirmed,ipnConfirmed,ipnConfirmed?"IPN":null);
        }
    }
    public Config config() { return new Config(configured(),"VNPAY_SANDBOX"); }
    boolean configured() {
        if(!enabled || !terminal.matches("[A-Za-z0-9]{8}") || secret.length()<20 || !paymentUrl.equals(SANDBOX_URL)) return false;
        try { var uri=URI.create(returnUrl);
            boolean local=allowLocalReturn && "http".equals(uri.getScheme()) && uri.getHost()!=null
                    && Set.of("localhost","127.0.0.1").contains(uri.getHost()) && uri.getPort()==5174;
            return ("https".equals(uri.getScheme()) || local) && uri.getHost()!=null
                && uri.getUserInfo()==null && uri.getQuery()==null && uri.getFragment()==null
                && uri.getPath().equals("/thanh-toan-vnpay/ket-qua"); }
        catch(IllegalArgumentException e) { return false; }
    }
    private void requireConfigured() { if(!configured()) throw AppException.badRequest("VNPAY Sandbox chưa được cấu hình đầy đủ trên máy chủ"); }
    public CommerceDtos.Quote quote(Long owner,CommerceDtos.Request request) {
        requireConfigured(); var quote=sessions.gatewayQuote(owner,request);
        if(quote.total()!=null && !validAmount(quote.total())) throw AppException.badRequest("Số tiền thử VNPAY cần là VND nguyên dương, tối đa 9.999.999.999đ");
        return quote;
    }
    public Start create(Long owner,CommerceDtos.Request request,String clientIp) {
        requireConfigured();
        if(!validAmount(request.expectedTotal())) throw AppException.badRequest("Tổng tiền thử VNPAY không hợp lệ");
        var session=sessions.createGateway(owner,request);
        return new Start(session.code(),session.status(),session.status().equals("AWAITING_GATEWAY")?paymentUrl(session,clientIp):null);
    }
    public DemoCardPayment.View owned(Long owner,String code) { requireConfigured(); return sessions.gatewayOwner(owner,code); }
    public Result ownedResult(Long owner,String code) {
        var snapshot=owned(owner,code);
        boolean paid="PAID_SANDBOX".equals(snapshot.status());
        var posted=publish(snapshot);
        String confirmation=snapshot.confirmationMethod();
        if(paid && confirmation==null) confirmation="IPN";
        return new Result(true,snapshot.code(),snapshot.status(),snapshot.message(),snapshot.amount(),snapshot.paidAt(),
                snapshot.expiresAt(),snapshot.serverTime(),paid && "IPN".equals(confirmation),paid,confirmation,posted.code(),posted.message());
    }
    private record Published(String code,String message) {}
    private Published publish(DemoCardPayment.View snapshot) {
        if(orders==null || !"PAID_SANDBOX".equals(snapshot.status())) return new Published(null,null);
        try { return new Published(orders.post(snapshot.code()).code(),null); }
        catch(AppException e) { return new Published(null,"VNPAY đã xác nhận thanh toán. Chưa thể ghi đơn hàng: "+e.getMessage()); }
        catch(Exception e) { return new Published(null,"VNPAY đã xác nhận thanh toán; chưa ghi được đơn vào hệ thống. Vui lòng kiểm tra lại giao dịch này hoặc liên hệ cửa hàng."); }
    }
    static boolean validAmount(BigDecimal amount) {
        return OnlineBankPayment.validAmount(amount) && amount.compareTo(new BigDecimal("9999999999"))<=0;
    }
    String paymentUrl(DemoCardPayment.View session,String clientIp) {
        var p=new TreeMap<String,String>();
        p.put("vnp_Version","2.1.0"); p.put("vnp_Command","pay"); p.put("vnp_TmnCode",terminal);
        p.put("vnp_Amount",session.amount().movePointRight(2).toBigIntegerExact().toString());
        p.put("vnp_CurrCode","VND"); p.put("vnp_TxnRef",session.code()); p.put("vnp_OrderInfo","SMASHSTEP SANDBOX "+session.code());
        p.put("vnp_OrderType","other"); p.put("vnp_Locale","vn"); p.put("vnp_BankCode","NCB");
        p.put("vnp_IpAddr",clientIp!=null && clientIp.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")?clientIp:"127.0.0.1");
        p.put("vnp_ReturnUrl",returnUrl);
        p.put("vnp_CreateDate",DATE.format(session.createdAt().atZone(ZONE)));
        p.put("vnp_ExpireDate",DATE.format(session.expiresAt().atZone(ZONE)));
        String query=encoded(p); return paymentUrl+"?"+query+"&vnp_SecureHash="+hmac(secret,query);
    }
    static String encoded(Map<String,String> fields) {
        var parts=new ArrayList<String>();
        new TreeMap<>(fields).forEach((key,value)->{ if(value!=null && !value.isEmpty()) parts.add(
                URLEncoder.encode(key,StandardCharsets.UTF_8)+"="+URLEncoder.encode(value,StandardCharsets.UTF_8)); });
        return String.join("&",parts);
    }
    static String hmac(String secret,String data) {
        try { var mac=Mac.getInstance("HmacSHA512"); mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA512"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8))); }
        catch(java.security.GeneralSecurityException e) { throw new IllegalStateException("Cannot sign VNPAY sandbox request"); }
    }
    private Map<String,String> verified(Map<String,String[]> raw) {
        if(!configured() || raw==null || raw.size()>40) return null;
        var fields=new TreeMap<String,String>(); String hash=null;
        for(var entry:raw.entrySet()) {
            if(!entry.getKey().startsWith("vnp_")) continue;
            var values=entry.getValue();
            if(values==null || values.length!=1 || values[0]==null || values[0].length()>1000) return null;
            if(entry.getKey().equals("vnp_SecureHash")) hash=values[0];
            else if(!entry.getKey().equals("vnp_SecureHashType")) fields.put(entry.getKey(),values[0]);
        }
        if(hash==null || !hash.matches("[0-9a-fA-F]{128}") || !terminal.equals(fields.get("vnp_TmnCode"))) return null;
        if(!MessageDigest.isEqual(hmac(secret,encoded(fields)).getBytes(StandardCharsets.US_ASCII),hash.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII))) return null;
        if(!Objects.toString(fields.get("vnp_TxnRef"),"").matches("SB[A-F0-9]{32}")
                || !Objects.toString(fields.get("vnp_Amount"),"").matches("[0-9]{1,12}")) return null;
        return fields;
    }
    public Map<String,String> ipn(Map<String,String[]> raw) {
        var fields=verified(raw); if(fields==null) return ack("97","Invalid signature");
        if(!Objects.toString(fields.get("vnp_ResponseCode"),"").matches("[0-9]{2}")
                || !Objects.toString(fields.get("vnp_TransactionStatus"),"").matches("[0-9]{2}")) return ack("99","Invalid response fields");
        boolean success="00".equals(fields.get("vnp_ResponseCode")) && "00".equals(fields.get("vnp_TransactionStatus"));
        Instant paidAt=null;
        if(success) {
            if(!Objects.toString(fields.get("vnp_TransactionNo"),"").matches("[0-9]{1,30}")) return ack("99","Invalid transaction reference");
            try { paidAt=LocalDateTime.parse(fields.get("vnp_PayDate"),DATE).atZone(ZONE).toInstant(); }
            catch(java.time.DateTimeException|NullPointerException e) { return ack("99","Invalid payment date"); }
            if(paidAt.isAfter(Instant.now().plusSeconds(60))) return ack("99","Invalid payment time");
        }
        String code=sessions.confirmGateway(fields.get("vnp_TxnRef"),new BigDecimal(fields.get("vnp_Amount")).movePointLeft(2),
                success,"VNPAY-SANDBOX-"+fields.get("vnp_TransactionNo"),paidAt);
        if(orders!=null && ("00".equals(code) || "02".equals(code))) {
            var snapshot=sessions.gatewaySnapshot(fields.get("vnp_TxnRef"));
            if(snapshot!=null && "PAID_SANDBOX".equals(snapshot.status()) && publish(snapshot).code()==null)
                return ack("99","Invoice publication pending");
        }
        return ack(code,switch(code) { case "00"->"Confirm Success"; case "01"->"Order not found"; case "02"->"Order already confirmed"; case "04"->"Invalid amount"; default->"Cannot confirm payment"; });
    }
    private Map<String,String> ack(String code,String message) { return Map.of("RspCode",code,"Message",message); }
    /** Browser return checks integrity and reads IPN state; it never confirms a transaction. */
    public Result returned(Map<String,String[]> raw) {
        var fields=verified(raw);
        if(fields==null) return new Result(false,null,"INVALID","Không xác minh được chữ ký kết quả VNPAY",null,null,null,Instant.now(),false);
        var snapshot=sessions.gatewaySnapshot(fields.get("vnp_TxnRef"));
        if(snapshot==null) return new Result(true,fields.get("vnp_TxnRef"),"UNKNOWN","Không tìm thấy phiên thử; backend có thể đã khởi động lại",null,null,null,Instant.now(),false);
        if(snapshot.amount().compareTo(new BigDecimal(fields.get("vnp_Amount")).movePointLeft(2))!=0)
            return new Result(false,snapshot.code(),"INVALID","Số tiền kết quả không khớp phiên thử",null,null,null,Instant.now(),false);
        boolean paid=snapshot.status().equals("PAID_SANDBOX"); String message=snapshot.message(),status=snapshot.status();
        if((status.equals("AWAITING_GATEWAY") || status.equals("EXPIRED")) && "00".equals(fields.get("vnp_ResponseCode")) && "00".equals(fields.get("vnp_TransactionStatus")))
            message="Đang xác nhận thanh toán với VNPAY. Vui lòng chờ trong giây lát.";
        else if(!paid && !"00".equals(fields.get("vnp_ResponseCode"))) { status="RETURN_FAILED"; message="VNPAY trả về giao dịch thử không thành công (mã "+fields.get("vnp_ResponseCode")+")."; }
        String confirmation=snapshot.confirmationMethod();
        if(paid && confirmation==null) confirmation="IPN";
        var posted=publish(snapshot);
        return new Result(true,snapshot.code(),status,message,snapshot.amount(),snapshot.paidAt(),snapshot.expiresAt(),snapshot.serverTime(),
                paid && "IPN".equals(confirmation),paid,confirmation,posted.code(),posted.message());
    }
}
