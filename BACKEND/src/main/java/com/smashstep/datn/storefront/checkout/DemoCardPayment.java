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

/** Local simulation only. Never calls checkout, persists a receipt, or changes inventory. */
@Service
public class DemoCardPayment {
    private final CustomerCommerceService commerce;
    private final boolean enabled;
    private final Clock clock;
    private final Map<String,Session> sessions = new HashMap<>();
    private static final Duration LIFETIME = Duration.ofMinutes(5), RETENTION = Duration.ofHours(24);
    private static final Map<String,String> CARDS = Map.of(
            "9704198526191432198", "OK", "9704195798459170488", "Thẻ thử không đủ số dư",
            "9704192181368742", "Thẻ thử chưa kích hoạt", "9704193370791314", "Thẻ thử bị khóa",
            "9704194841945513", "Thẻ thử đã hết hạn");

    @Autowired
    public DemoCardPayment(CustomerCommerceService commerce, @Value("${app.payment.demo.enabled:false}") boolean enabled) {
        this(commerce,enabled,Clock.systemUTC());
    }
    DemoCardPayment(CustomerCommerceService commerce,boolean enabled,Clock clock) {
        this.commerce=commerce; this.enabled=enabled; this.clock=clock;
    }
    public record Config(boolean enabled) {}
    public record Card(@NotBlank @Pattern(regexp="[0-9 ]{16,24}") String number,
            @NotBlank @Size(max=50) String holder, @NotBlank @Pattern(regexp="[0-9]{2}/[0-9]{2}") String issueDate) {}
    public record Otp(@NotBlank @Size(max=36) String challenge, @NotBlank @Pattern(regexp="[0-9]{6}") String otp) {}
    public record View(String code,String status,String message,
            @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) BigDecimal amount,
            BigDecimal subtotal,BigDecimal discount,BigDecimal shipping,Receiver receiver,List<Line> lines,
            Instant createdAt,Instant expiresAt,Instant serverTime,Instant paidAt,String reference,String challenge) {}
    private static final class Session {
        final String code,requestId; final Long owner; final Request request; final Quote quote; final Instant createdAt,expiresAt;
        String status="AWAITING_CARD",message="Nhập thông tin thẻ thử NCB",challenge,reference; Instant paidAt; int failures;
        Session(Long owner,Request request,Quote quote,Instant now) {
            this.owner=owner;
            this.request=new Request(List.copyOf(request.items()),request.voucherCode(),request.paymentMethodId(),request.receiver(),
                    request.note(),request.requestId(),request.expectedTotal(),request.saveAddress(),request.makeDefaultAddress());
            this.requestId=request.requestId(); this.quote=quote;
            this.code="DEMO-"+UUID.randomUUID(); this.createdAt=now; this.expiresAt=now.plus(LIFETIME);
        }
    }
    public Config config() { return new Config(enabled); }
    private void requireEnabled() {
        if(!enabled) throw AppException.notFound("Chế độ thanh toán giả lập chưa được bật trên máy chủ này");
    }
    public Quote quote(Long owner,Request request) { requireEnabled(); return commerce.demoQuote(owner,request); }
    public View create(Long owner,Request request) {
        requireEnabled(); commerce.customer(owner);
        if(request.requestId()==null || !request.requestId().matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
            throw AppException.badRequest("Mã phiên thanh toán thử không hợp lệ");
        if(request.receiver()==null || request.receiver().address().length()>500) throw AppException.badRequest("Cần thông tin nhận hàng đầy đủ");
        synchronized(sessions) {
            cleanup();
            var prior=prior(owner,request); if(prior!=null) return view(prior);
            capacity(owner);
        }
        var quoted=commerce.demoQuote(owner,request);
        if(!quoted.canCheckout()) throw AppException.badRequest(quoted.message());
        if(request.expectedTotal()==null || request.expectedTotal().compareTo(quoted.total())!=0)
            throw AppException.conflict("Giá hoặc phí vận chuyển vừa thay đổi. Vui lòng kiểm tra lại tổng tiền thử.");
        synchronized(sessions) {
            cleanup(); var prior=prior(owner,request); if(prior!=null) return view(prior);
            capacity(owner); var session=new Session(owner,request,quoted,clock.instant()); sessions.put(session.code,session);
            return view(session);
        }
    }
    private Session prior(Long owner,Request request) {
        for(var s:sessions.values()) if(s.owner.equals(owner) && s.requestId.equals(request.requestId())) {
            if(!s.request.equals(request)) throw AppException.conflict("Phiên thử đã dùng cho một giỏ hàng khác");
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
        synchronized(sessions) { cleanup(); return view(owned(owner,code)); }
    }
    public View card(Long owner,String code,Card card) {
        requireEnabled(); commerce.customer(owner);
        synchronized(sessions) {
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
            var s=owned(owner,code); expire(s);
            if(!s.status.equals("AWAITING_OTP")) return view(s);
            if(!Objects.equals(s.challenge,otp.challenge()) || !"123456".equals(otp.otp())) {
                failure(s,"OTP thử không đúng. Vui lòng dùng 123456"); return view(s);
            }
            s.status="PAID_DEMO"; s.message="Thanh toán thử thành công. Không có tiền thật được thu.";
            s.paidAt=clock.instant(); s.reference="SIM-"+UUID.randomUUID(); s.challenge=null; return view(s);
        }
    }
    public View cancel(Long owner,String code) {
        requireEnabled(); commerce.customer(owner);
        synchronized(sessions) {
            var s=owned(owner,code); expire(s);
            if(pending(s)) { s.status="CANCELED"; s.message="Đã hủy giao dịch thử"; s.challenge=null; }
            return view(s);
        }
    }
    public List<View> paidOrders() {
        if(!enabled) return List.of();
        synchronized(sessions) {
            cleanup(); return sessions.values().stream().filter(s->s.status.equals("PAID_DEMO"))
                    .sorted(Comparator.comparing((Session s)->s.paidAt).reversed()).map(this::view).toList();
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
    private boolean pending(Session s) { return s.status.equals("AWAITING_CARD") || s.status.equals("AWAITING_OTP"); }
    private void expire(Session s) {
        if(pending(s) && !clock.instant().isBefore(s.expiresAt)) { s.status="EXPIRED"; s.message="Phiên thanh toán thử đã hết hạn 5 phút"; s.challenge=null; }
    }
    private void cleanup() {
        sessions.values().forEach(this::expire);
        sessions.values().removeIf(s->s.status.equals("PAID_DEMO") ? !clock.instant().isBefore(s.paidAt.plus(RETENTION))
                : !clock.instant().isBefore(s.expiresAt.plus(Duration.ofMinutes(10))));
    }
    private View view(Session s) {
        expire(s);
        return new View(s.code,s.status,s.message,s.quote.total(),s.quote.subtotal(),s.quote.discount(),s.quote.shipping(),
                s.request.receiver(),s.quote.lines(),s.createdAt,s.expiresAt,clock.instant(),s.paidAt,s.reference,s.challenge);
    }
}
