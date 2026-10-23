package com.smashstep.datn.storefront.checkout;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.employee.entity.NhanVien;
import com.smashstep.datn.invoice.entity.HoaDon;
import com.smashstep.datn.invoice.entity.LichSuHoaDon;
import com.smashstep.datn.invoice.entity.LichSuThanhToan;
import com.smashstep.datn.invoice.service.InvoiceCodeService;
import com.smashstep.datn.sales.service.PosActor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/** QR instructions never constitute a receipt. Only an authenticated employee can record received money. */
@Service
@Transactional(readOnly = true)
public class OnlineBankPayment {
    private final EntityManager em;
    private final InvoiceCodeService codes;
    private final String bank, account, name, label;
    private final boolean enabled;
    private final Clock clock;
    static final Duration QR_LIFETIME = Duration.ofMinutes(5);

    @Autowired
    public OnlineBankPayment(EntityManager em, InvoiceCodeService codes,
            @Value("${app.payment.qr.enabled:true}") boolean enabled,
            @Value("${app.payment.qr.bank-bin:970422}") String bank,
            @Value("${app.payment.qr.account:0966026145}") String account,
            @Value("${app.payment.qr.account-name:VU CHI TUAN ANH}") String name,
            @Value("${app.payment.qr.bank-label:MB Bank}") String label) {
        this(em, codes, enabled, bank, account, name, label, Clock.systemDefaultZone());
    }
    OnlineBankPayment(EntityManager em, InvoiceCodeService codes, boolean enabled,
            String bank, String account, String name, String label, Clock clock) {
        this.em = em; this.codes = codes; this.enabled = enabled;
        this.bank = bank.trim(); this.account = account.trim(); this.name = normalizeAccountName(name); this.label = label.trim();
        this.clock = clock;
    }

    public record Payment(String methodCode, String status, String message,
            @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
            BigDecimal amount, String bankLabel, String accountNumber, String accountName,
            String memo, String qrUrl, LocalDateTime paidAt, boolean payable,
            Instant expiresAt, Instant serverTime) {}

    public record Confirmation(@NotNull @DecimalMin("1") @Digits(integer = 13, fraction = 0) BigDecimal amount,
            @NotBlank @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._/\\-]{3,99}",
                    message = "Mã giao dịch ngân hàng từ 4 đến 100 ký tự, chỉ gồm chữ, số, dấu chấm, gạch nối, gạch chéo hoặc gạch dưới") String reference,
            @NotNull @AssertTrue(message = "Cần xác nhận đã nhận đúng tiền trên tài khoản ngân hàng") Boolean received) {}

    public boolean configured() {
        return enabled && bank.matches("[0-9]{6}") && account.matches("[A-Za-z0-9]{1,19}")
                && name.matches("[A-Z]+(?: [A-Z]+)*") && name.length() >= 5 && name.length() <= 50
                && !label.isBlank() && label.length() <= 100;
    }
    static String normalizeAccountName(String value) {
        return Normalizer.normalize(Objects.requireNonNullElse(value, ""), Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D').toUpperCase(Locale.ROOT).strip().replaceAll("\\s+", " ");
    }
    Instant expiresAt(HoaDon h) {
        return h.getNgayTao() == null ? null : h.getNgayTao().atZone(clock.getZone()).toInstant().plus(QR_LIFETIME);
    }
    public static boolean validAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || amount.compareTo(new BigDecimal("9999999999999")) > 0) return false;
        try { amount.setScale(0, RoundingMode.UNNECESSARY); return true; }
        catch (ArithmeticException e) { return false; }
    }
    public static boolean bankOrder(EntityManager em, HoaDon h) {
        return h.getIdPhuongThucThanhToan() != null
                && "CHUYEN_KHOAN".equals(h.getIdPhuongThucThanhToan().getMaPhuongThuc())
                && OnlineOrderInventory.managed(em, h);
    }
    public static boolean hasReceipt(EntityManager em, HoaDon h) {
        return h.getNgayThanhToan() != null || em.createQuery(
                "select count(p) from LichSuThanhToan p where p.idHoaDon.id=:id and p.trangThai in (1,2)", Long.class)
                .setParameter("id", h.getId()).getSingleResult() > 0;
    }
    public String memo(HoaDon h) {
        String code = h.getMaHoaDon();
        if (code == null || !code.matches("HD[0-9]{1,48}")) throw AppException.conflict("Mã đơn hàng không hợp lệ để tạo QR");
        return code.length() > 40 ? code : "SMASHSTEP " + code;
    }
    public Payment details(HoaDon h) {
        String method = h.getIdPhuongThucThanhToan() == null ? null : h.getIdPhuongThucThanhToan().getMaPhuongThuc();
        if ("VNPAY".equals(method) && OnlineOrderInventory.managed(em,h) && h.getNgayThanhToan()!=null)
            return result(method,"PAID","Đã thanh toán bằng VNPAY",h,false);
        if (!bankOrder(em, h)) return result(method, "OTHER", "", h, false);
        var receipts = em.createQuery("select p from LichSuThanhToan p where p.idHoaDon.id=:id", LichSuThanhToan.class)
                .setParameter("id", h.getId()).getResultList();
        if (receipts.stream().anyMatch(p -> Integer.valueOf(2).equals(p.getTrangThai())))
            return result(method, "REFUNDED", "Đơn đã ghi nhận hoàn tiền. Không chuyển thêm tiền.", h, false);
        BigDecimal received = receipts.stream().filter(p -> Integer.valueOf(1).equals(p.getTrangThai()))
                .map(p -> Objects.requireNonNullElse(p.getSoTien(), BigDecimal.ZERO)).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (h.getNgayThanhToan() != null && h.getThanhTien() != null && received.compareTo(h.getThanhTien()) == 0)
            return result(method, "PAID", "Cửa hàng đã xác nhận nhận đủ tiền. Không thanh toán lại.", h, false);
        if (h.getNgayThanhToan() != null || received.signum() != 0)
            return result(method, "REVIEW", "Khoản thanh toán cần cửa hàng đối soát. Không chuyển thêm tiền.", h, false);
        if (h.getTrangThai() == null || h.getTrangThai() < 0 || h.getTrangThai() > 4)
            return result(method, "CLOSED", "Đơn hiện không nhận thanh toán. Nếu đã chuyển tiền, liên hệ cửa hàng để đối soát.", h, false);
        Instant expiry = expiresAt(h);
        if (expiry == null) return result(method, "UNAVAILABLE", "Thiếu thời điểm tạo đơn để xác định hạn thanh toán. Vui lòng liên hệ cửa hàng.", h, false);
        if (!clock.instant().isBefore(expiry))
            return result(method, "EXPIRED", "Đã hết 5 phút thanh toán. Mã QR đã được ẩn. Nếu đã chuyển tiền, chờ cửa hàng đối soát và không chuyển lại.", h, false);
        if (!configured() || !validAmount(h.getThanhTien()))
            return result(method, "UNAVAILABLE", "Chưa thể tạo QR cho đơn này. Vui lòng liên hệ cửa hàng.", h, false);
        return result(method, "PENDING", "Chờ cửa hàng đối soát tiền vào. Nếu đã chuyển khoản, không thanh toán lại.", h, true);
    }
    private Payment result(String method, String status, String message, HoaDon h, boolean payable) {
        String description = payable ? memo(h) : null;
        String url = payable ? UriComponentsBuilder.fromUriString("https://img.vietqr.io/image/" + bank + "-" + account + "-compact2.svg")
                .queryParam("amount", h.getThanhTien().setScale(0, RoundingMode.UNNECESSARY).toPlainString())
                .queryParam("addInfo", description).queryParam("accountName", name).build().encode().toUriString() : null;
        BigDecimal amount = validAmount(h.getThanhTien()) ? h.getThanhTien().setScale(0, RoundingMode.UNNECESSARY) : h.getThanhTien();
        return new Payment(method, status, message, amount, payable ? label : null,
                payable ? account : null, payable ? name : null, description, url, h.getNgayThanhToan(), payable,
                "CHUYEN_KHOAN".equals(method) ? expiresAt(h) : null, clock.instant());
    }
    public Payment staffDetails(String code) { return details(invoice(code, false)); }

    @Transactional
    public Payment confirm(String code, Confirmation request) {
        Long actor = PosActor.employeeId();
        var employee = em.find(NhanVien.class, actor);
        if (employee == null || !Integer.valueOf(1).equals(employee.getTrangThai())) throw AppException.forbidden("Nhân viên không hoạt động");
        if (request == null || !Boolean.TRUE.equals(request.received()) || !validAmount(request.amount())
                || request.reference() == null || !request.reference().matches("[A-Za-z0-9][A-Za-z0-9._/\\-]{3,99}"))
            throw AppException.badRequest("Cần số tiền, mã giao dịch ngân hàng và xác nhận đã thực nhận tiền");
        codes.lockInvoiceCreation();
        var h = invoice(code, true);
        if (!bankOrder(em, h)) throw AppException.conflict("Chỉ xác nhận chuyển khoản cho đơn online chọn QR");
        if (!validAmount(h.getThanhTien()) || request.amount().compareTo(h.getThanhTien()) != 0)
            throw AppException.conflict("Số tiền phải khớp toàn bộ tổng đơn hàng hợp lệ hiện tại");
        String reference = request.reference().toUpperCase(Locale.ROOT);
        var duplicate = em.createQuery("select p from LichSuThanhToan p where upper(p.maGiaoDich)=:ref", LichSuThanhToan.class)
                .setParameter("ref", reference).getResultList();
        if (!duplicate.isEmpty()) {
            if (duplicate.size() == 1 && duplicate.get(0).getIdHoaDon().getId().equals(h.getId())
                    && Integer.valueOf(1).equals(duplicate.get(0).getTrangThai())
                    && duplicate.get(0).getSoTien()!=null && request.amount().compareTo(duplicate.get(0).getSoTien()) == 0
                    && "PAID".equals(details(h).status())) return details(h);
            throw AppException.conflict("Mã giao dịch này đã được ghi nhận; không được sử dụng lại");
        }
        if (h.getTrangThai()==null || h.getTrangThai()<0 || h.getTrangThai()>4 || hasReceipt(em, h))
            throw AppException.conflict("Đơn không còn ở trạng thái chờ thanh toán QR");
        // Expired QR instructions do not cancel money already received: staff still reconcile the actual bank receipt.
        var now = LocalDateTime.now(clock); var receipt = new LichSuThanhToan();
        receipt.setIdHoaDon(h); receipt.setIdPhuongThucThanhToan(h.getIdPhuongThucThanhToan());
        receipt.setSoTien(h.getThanhTien()); receipt.setMaGiaoDich(reference); receipt.setTrangThai(1); receipt.setThoiGian(now);
        receipt.setMoTa("Nhân viên " + actor + " đối soát đã nhận chuyển khoản; nội dung " + memo(h)); em.persist(receipt);
        h.setNgayThanhToan(now); h.setNgayCapNhat(now);
        var event = new LichSuHoaDon(); event.setIdHoaDon(h); event.setNguoiTao(actor); event.setTrangThai(h.getTrangThai());
        event.setNgayTao(now); event.setGhiChu("Đã nhận đủ tiền chuyển khoản QR; giao dịch " + reference); em.persist(event);
        em.flush(); return details(h);
    }
    private HoaDon invoice(String code, boolean lock) {
        var query = em.createQuery("select h from HoaDon h where h.maHoaDon=:code", HoaDon.class).setParameter("code", code);
        if (lock) query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        return query.getResultStream().findFirst().orElseThrow(() -> AppException.notFound("Không tìm thấy hóa đơn"));
    }
}
