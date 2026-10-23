package com.smashstep.datn.storefront.checkout;

import com.smashstep.datn.common.exception.AppException;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import static com.smashstep.datn.storefront.checkout.CommerceDtos.*;

/** Test-only sessions (local simulator or external VNPAY Sandbox). Never writes financial data. */
@Service
public class DemoCardPayment {
    private final CustomerCommerceService commerce;
    private final boolean enabled;
    private final Clock clock;
    private final DemoPaymentLedger ledger;
    private final Map<String,Session> sessions = new HashMap<>();
    private static final Duration LIFETIME = Duration.ofMinutes(5), RETENTION = Duration.ofHours(24);
    private static final Map<String,String> CARDS = Map.of(
            "9704198526191432198", "OK", "9704195798459170488", "Thẻ thử không đủ số dư",
            "9704192181368742", "Thẻ thử chưa kích hoạt", "9704193370791314", "Thẻ thử bị khóa",
            "9704194841945513", "Thẻ thử đã hết hạn");

    @Autowired
    public DemoCardPayment(CustomerCommerceService commerce, @Value("${app.payment.demo.enabled:false}") boolean enabled,
            DemoPaymentLedger ledger) {
        this(commerce,enabled,Clock.systemUTC(),ledger);
    }
    DemoCardPayment(CustomerCommerceService commerce,boolean enabled,Clock clock) {
        this(commerce,enabled,clock,DemoPaymentLedger.inMemory());
    }
    private DemoCardPayment(CustomerCommerceService commerce,boolean enabled,Clock clock,DemoPaymentLedger ledger) {
        this.commerce=commerce; this.enabled=enabled; this.clock=clock; this.ledger=ledger;
    }
    public record Config(boolean enabled) {}
    public record Account(@NotBlank @Size(max=50) String username, @NotBlank @Size(max=100) String password) {}
    public record Card(@NotBlank @Pattern(regexp="[0-9 ]{16,24}") String number,
            @NotBlank @Size(max=50) String holder, @NotBlank @Pattern(regexp="[0-9]{2}/[0-9]{2}") String issueDate) {}
    public record Otp(@NotBlank @Size(max=36) String challenge, @NotBlank @Pattern(regexp="[0-9]{6}") String otp) {}
    public record View(String code,String status,String message,
            @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) BigDecimal amount,
            BigDecimal subtotal,BigDecimal discount,BigDecimal shipping,Receiver receiver,List<Line> lines,
            Instant createdAt,Instant expiresAt,Instant serverTime,Instant paidAt,String reference,String challenge,String channel,
            String confirmationMethod) {
        public View(String code,String status,String message,BigDecimal amount,BigDecimal subtotal,BigDecimal discount,
                BigDecimal shipping,Receiver receiver,List<Line> lines,Instant createdAt,Instant expiresAt,Instant serverTime,
                Instant paidAt,String reference,String challenge,String channel) {
            this(code,status,message,amount,subtotal,discount,shipping,receiver,lines,createdAt,expiresAt,serverTime,paidAt,
                    reference,challenge,channel,"PAID_SANDBOX".equals(status)?"IPN":"PAID_DEMO".equals(status)?"OTP":null);
        }
    }
    private static final class Session {
        final String code,requestId,channel; final Long owner; final Request request; final Quote quote; final Instant createdAt,expiresAt;
        String status="AWAITING_CARD",message="Nhập tài khoản thử hoặc thẻ NCB thử, sau đó xác thực OTP",challenge,reference,confirmationMethod; Instant paidAt; int failures;
        Session(Long owner,Request request,Quote quote,Instant now,String channel) {
            this.owner=owner;
            this.request=new Request(List.copyOf(request.items()),request.voucherCode(),request.paymentMethodId(),request.receiver(),
                    request.note(),request.requestId(),request.expectedTotal(),request.saveAddress(),request.makeDefaultAddress(),request.deliveryPoint(),request.receiveOrderEmails());
            this.requestId=request.requestId(); this.quote=quote;
            this.channel=channel;
            this.code="VNPAY_SANDBOX".equals(channel)?"SB"+UUID.randomUUID().toString().replace("-","").toUpperCase(Locale.ROOT):"DEMO-"+UUID.randomUUID();
            this.createdAt=now; this.expiresAt=now.plus(LIFETIME);
            if("VNPAY_SANDBOX".equals(channel)) { status="AWAITING_GATEWAY"; message="Chờ xác nhận thanh toán từ VNPAY Sandbox"; }
        }
    }
    public Config config() { return new Config(enabled); }
    private void requireEnabled() {
        if(!enabled) throw AppException.notFound("Chế độ thanh toán giả lập chưa được bật trên máy chủ này");
    }
    public Quote quote(Long owner,Request request) { requireEnabled(); return commerce.demoQuote(owner,request); }
    public View create(Long owner,Request request) {
        requireEnabled(); return createSession(owner,request,"LOCAL");
    }
    // Only the gateway service may create or confirm external sessions.
    View createGateway(Long owner,Request request) { return createSession(owner,request,"VNPAY_SANDBOX"); }
    Quote gatewayQuote(Long owner,Request request) { return commerce.demoQuote(owner,request); }
    private View createSession(Long owner,Request request,String channel) {
        commerce.customer(owner);
        if(request.requestId()==null || !request.requestId().matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
            throw AppException.badRequest("Mã phiên thanh toán thử không hợp lệ");
        if(request.receiver()==null || request.receiver().address().length()>500) throw AppException.badRequest("Cần thông tin nhận hàng đầy đủ");
        synchronized(sessions) {
            cleanup();
            var saved=ledger.prior(owner,request,channel); if(saved!=null) return current(saved);
            var prior=prior(owner,request,channel); if(prior!=null) return view(prior);
            capacity(owner);
        }
        var quoted=commerce.demoQuote(owner,request);
        if(!quoted.canCheckout()) throw AppException.badRequest(quoted.message());
        commerce.validateQuotedOffer(owner,request.items(),quoted);
        // Persist the voucher that was actually quoted, so a confirmed receipt always posts the same discount.
        if(!Objects.equals(voucherCode(request.voucherCode()),voucherCode(quoted.voucherCode())))
            throw AppException.conflict("Phiếu giảm giá vừa thay đổi. Vui lòng cập nhật ưu đãi trước khi thanh toán.");
        if(request.expectedTotal()==null || request.expectedTotal().compareTo(quoted.total())!=0)
            throw AppException.conflict("Giá hoặc phí vận chuyển vừa thay đổi. Vui lòng kiểm tra lại tổng tiền thử.");
        synchronized(sessions) {
            cleanup(); var saved=ledger.prior(owner,request,channel); if(saved!=null) return current(saved);
            var prior=prior(owner,request,channel); if(prior!=null) return view(prior);
            capacity(owner); var session=new Session(owner,request,quoted,clock.instant(),channel); sessions.put(session.code,session);
            return view(session);
        }
    }
    private static String voucherCode(String code) {
        return code==null || code.isBlank()?null:code.trim().toLowerCase(Locale.ROOT);
    }
    private Session prior(Long owner,Request request,String channel) {
        for(var s:sessions.values()) if(s.owner.equals(owner) && s.requestId.equals(request.requestId())) {
            if(!s.request.equals(request) || !s.channel.equals(channel)) throw AppException.conflict("Phiên thử đã dùng cho một giỏ hàng hoặc cổng thanh toán khác");
            return s;
        }
        return null;
    }
    private void capacity(Long owner) {
        if(sessions.size()>=1000 || sessions.values().stream().filter(s->s.owner.equals(owner)).count()>=20)
            throw AppException.conflict("Đã đạt giới hạn phiên thử. Vui lòng chờ các phiên cũ hết hạn.");
    }
    public View get(Long owner,String code) {
        requireEnabled(); commerce.customer(owner);
        synchronized(sessions) { cleanup(); var saved=ledger.owned(owner,code); return saved!=null?current(saved):view(owned(owner,code)); }
    }
    public View account(Long owner,String code,Account account) {
        requireEnabled(); commerce.customer(owner);
        synchronized(sessions) {
            var saved=ledger.owned(owner,code); if(saved!=null) return current(saved);
            var s=owned(owner,code); expire(s);
            if(!s.status.equals("AWAITING_CARD")) return view(s);
            // Deliberately public simulator credentials, unrelated to bank or application accounts.
            if(!"smashstep_test".equals(account.username()) || !"Test@123456".equals(account.password())) {
                failure(s,"Tài khoản hoặc mật khẩu thử chưa đúng. Dùng bộ tài khoản mẫu bên cạnh"); return view(s);
            }
            s.status="AWAITING_OTP"; s.message="Nhập OTP thử 123456 để xác thực giao dịch giả lập";
            s.challenge=UUID.randomUUID().toString(); return view(s);
        }
    }
    public View card(Long owner,String code,Card card) {
        requireEnabled(); commerce.customer(owner);
        synchronized(sessions) {
            var saved=ledger.owned(owner,code); if(saved!=null) return current(saved);
            var s=owned(owner,code); expire(s);
            if(!s.status.equals("AWAITING_CARD")) return view(s);
            String number=Objects.requireNonNullElse(card.number(),"").replace(" ","");
            String holder=Objects.requireNonNullElse(card.holder(),"").trim().replaceAll("\\s+"," ").toUpperCase(Locale.ROOT);
            if(!CARDS.containsKey(number) || !holder.equals("NGUYEN VAN A") || !"07/15".equals(card.issueDate())) {
                failure(s,"Chỉ chấp nhận thông tin thẻ thử công khai được hiển thị trên trang"); return view(s);
            }
            String result=CARDS.get(number);
            if(!result.equals("OK")) { s.status="FAILED"; s.message=result; return view(s); }
            s.status="AWAITING_OTP"; s.message="Nhập OTP thử 123456 để xác thực giao dịch giả lập";
            s.challenge=UUID.randomUUID().toString(); return view(s);
        }
    }
    public View otp(Long owner,String code,Otp otp) {
        requireEnabled(); commerce.customer(owner);
        synchronized(sessions) {
            var saved=ledger.owned(owner,code); if(saved!=null) return current(saved);
            var s=owned(owner,code); expire(s);
            if(!s.status.equals("AWAITING_OTP")) return view(s);
            if(!Objects.equals(s.challenge,otp.challenge()) || !"123456".equals(otp.otp())) {
                failure(s,"OTP thử không đúng. Vui lòng dùng 123456"); return view(s);
            }
            return complete(s,"PAID_DEMO",clock.instant(),"SIM-"+UUID.randomUUID(),
                    "Thanh toán thử thành công. Đơn thử đã được lưu trong quản trị. Không thu tiền thật.");
        }
    }
    public View cancel(Long owner,String code) {
        requireEnabled(); commerce.customer(owner);
        synchronized(sessions) {
            var saved=ledger.owned(owner,code); if(saved!=null) return current(saved);
            var s=owned(owner,code); expire(s);
            if(pending(s)) { s.status="CANCELED"; s.message="Đã hủy giao dịch thử"; s.challenge=null; }
            return view(s);
        }
    }
    public List<View> paidOrders() {
        synchronized(sessions) {
            cleanup(); return ledger.paidOrders().stream().map(this::current).toList();
        }
    }
    private Session owned(Long owner,String code) {
        var s=sessions.get(code);
        if(s==null || !s.owner.equals(owner)) throw AppException.notFound("Không tìm thấy phiên thử của bạn; máy chủ có thể đã khởi động lại");
        return s;
    }
    private void failure(Session s,String message) {
        s.failures++; s.message=message+" (còn "+Math.max(0,5-s.failures)+" lần thử)";
        if(s.failures>=5) { s.status="FAILED"; s.challenge=null; }
    }
    private boolean paid(Session s) { return s.status.equals("PAID_DEMO") || s.status.equals("PAID_SANDBOX"); }
    private boolean pending(Session s) { return s.status.equals("AWAITING_CARD") || s.status.equals("AWAITING_OTP") || s.status.equals("AWAITING_GATEWAY"); }
    private void expire(Session s) {
        if(pending(s) && !clock.instant().isBefore(s.expiresAt)) { s.status="EXPIRED"; s.message="Phiên thanh toán thử đã hết hạn 5 phút"; s.challenge=null; }
    }
    private void cleanup() {
        sessions.values().forEach(this::expire);
        sessions.values().removeIf(s->paid(s) ? !clock.instant().isBefore(s.paidAt.plus(RETENTION))
                : !clock.instant().isBefore(s.channel.equals("VNPAY_SANDBOX")?s.createdAt.plus(RETENTION):s.expiresAt.plus(Duration.ofMinutes(10))));
    }
    private View view(Session s) {
        expire(s);
        return new View(s.code,s.status,s.message,s.quote.total(),s.quote.subtotal(),s.quote.discount(),s.quote.shipping(),
                s.request.receiver(),s.quote.lines(),s.createdAt,s.expiresAt,clock.instant(),s.paidAt,s.reference,s.challenge,s.channel,s.confirmationMethod);
    }
    private View current(View p) {
        return new View(p.code(),p.status(),p.message(),p.amount(),p.subtotal(),p.discount(),p.shipping(),p.receiver(),
                p.lines(),p.createdAt(),p.expiresAt(),clock.instant(),p.paidAt(),p.reference(),null,p.channel(),p.confirmationMethod());
    }
    private View complete(Session s,String status,Instant paidAt,String reference,String message) {
        return complete(s,status,paidAt,reference,message,"LOCAL".equals(s.channel)?"OTP":"IPN");
    }
    private View complete(Session s,String status,Instant paidAt,String reference,String message,String confirmationMethod) {
        var result=new View(s.code,status,message,s.quote.total(),s.quote.subtotal(),s.quote.discount(),s.quote.shipping(),
                s.request.receiver(),s.quote.lines(),s.createdAt,s.expiresAt,clock.instant(),paidAt,reference,null,s.channel,confirmationMethod);
        // Persist before changing the session or returning success. A failed write is safe to retry.
        ledger.save(s.owner,s.request,result);
        s.status=status; s.message=message; s.paidAt=paidAt; s.reference=reference; s.challenge=null; s.confirmationMethod=confirmationMethod;
        return result;
    }
    View gatewayOwner(Long owner,String code) {
        commerce.customer(owner);
        synchronized(sessions) {
            var saved=ledger.owned(owner,code); if(saved!=null && "VNPAY_SANDBOX".equals(saved.channel())) return current(saved);
            var s=owned(owner,code); if(!s.channel.equals("VNPAY_SANDBOX")) throw AppException.notFound("Không tìm thấy phiên VNPAY Sandbox"); return view(s); }
    }
    View gatewaySnapshot(String code) {
        synchronized(sessions) { cleanup();
            var saved=ledger.find(code); if(saved!=null && "VNPAY_SANDBOX".equals(saved.channel())) return current(saved);
            var s=sessions.get(code); return s==null || !s.channel.equals("VNPAY_SANDBOX")?null:view(s); }
    }
    String confirmGateway(String code,BigDecimal amount,boolean success,String reference,Instant paidAt) {
        return confirmGateway(code,amount,success,reference,paidAt,"IPN");
    }
    String confirmGateway(String code,BigDecimal amount,boolean success,String reference,Instant paidAt,String confirmationMethod) {
        if(!Set.of("IPN","QUERYDR").contains(confirmationMethod)) return "99";
        synchronized(sessions) {
            cleanup(); var saved=ledger.find(code);
            if(saved!=null && "VNPAY_SANDBOX".equals(saved.channel())) return amount==null || amount.compareTo(saved.amount())!=0?"04":"02";
            var s=sessions.get(code);
            if(s==null || !s.channel.equals("VNPAY_SANDBOX")) return "01";
            if(amount==null || amount.compareTo(s.quote.total())!=0) return "04";
            if(paid(s) || s.status.equals("FAILED") || s.status.equals("CANCELED")) return "02";
            if(success) {
                // A delayed, signed IPN may confirm a payment made inside the original validity window.
                if(paidAt==null || paidAt.isBefore(s.createdAt.minusSeconds(1)) || !paidAt.isBefore(s.expiresAt)) return "99";
                if(reference==null || ledger.hasReference(reference) || sessions.values().stream().anyMatch(other->other!=s && reference.equals(other.reference))) return "99";
                complete(s,"PAID_SANDBOX",paidAt,reference,"VNPAY đã xác nhận thanh toán.",confirmationMethod);
            } else { s.status="FAILED"; s.message="VNPAY Sandbox thông báo giao dịch thử không thành công"; }
            return "00";
        }
    }
}
