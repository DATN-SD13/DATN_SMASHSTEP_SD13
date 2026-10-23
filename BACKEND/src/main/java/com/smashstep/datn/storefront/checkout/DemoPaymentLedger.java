package com.smashstep.datn.storefront.checkout;

import com.smashstep.datn.common.exception.AppException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Private, durable test receipts. Never stores credentials or writes business database tables. */
@Service
public class DemoPaymentLedger {
    private static final int MAX_ENTRIES = 1000;
    private static final long MAX_BYTES = 20L * 1024 * 1024;
    private final JsonMapper json = JsonMapper.builder().build();
    private final Path file;
    private List<Entry> entries;

    record Entry(Long owner, String requestId, String fingerprint, DemoCardPayment.View payment, CommerceDtos.Request request) {}
    record Snapshot(int version, List<Entry> entries) {}

    @Autowired
    public DemoPaymentLedger(@Value("${app.payment.demo.ledger-file}") String filename) {
        this.file = Path.of(filename).toAbsolutePath().normalize();
    }
    private DemoPaymentLedger(boolean memory) { this.file = null; this.entries = new ArrayList<>(); }
    static DemoPaymentLedger inMemory() { return new DemoPaymentLedger(true); }

    synchronized DemoCardPayment.View prior(Long owner, CommerceDtos.Request request, String channel) {
        load();
        for (var e : entries) if (e.owner().equals(owner) && e.requestId().equals(request.requestId())) {
            if (!e.fingerprint().equals(fingerprint(request, channel)))
                throw AppException.conflict("Phiên thử đã dùng cho một giỏ hàng hoặc cổng thanh toán khác");
            return e.payment();
        }
        return null;
    }
    synchronized Entry entry(String code) {
        load();
        return entries.stream().filter(e -> e.payment().code().equals(code)).findFirst().orElse(null);
    }
    synchronized DemoCardPayment.View find(String code) {
        load();
        return entries.stream().map(Entry::payment).filter(p -> p.code().equals(code)).findFirst().orElse(null);
    }
    synchronized DemoCardPayment.View owned(Long owner, String code) {
        load();
        return entries.stream().filter(e -> e.owner().equals(owner) && e.payment().code().equals(code))
                .map(Entry::payment).findFirst().orElse(null);
    }
    synchronized boolean hasReference(String reference) {
        load();
        return entries.stream().anyMatch(e -> Objects.equals(reference, e.payment().reference()));
    }
    synchronized List<DemoCardPayment.View> paidOrders() {
        load();
        return entries.stream().map(Entry::payment)
                .sorted(Comparator.comparing(DemoCardPayment.View::paidAt).reversed()).toList();
    }
    synchronized void save(Long owner, CommerceDtos.Request request, DemoCardPayment.View payment) {
        load();
        var existing = prior(owner, request, payment.channel());
        if (existing != null) {
            if (!existing.code().equals(payment.code())) throw AppException.conflict("Phiên thử đã thanh toán trước đó");
            return;
        }
        if (!isPaid(payment) || hasReference(payment.reference()) || find(payment.code()) != null)
            throw AppException.conflict("Biên nhận thanh toán thử không hợp lệ hoặc đã tồn tại");
        if (entries.size() >= MAX_ENTRIES)
            throw AppException.conflict("Kho đơn thử đã đầy. Liên hệ quản trị để lưu trữ lịch sử trước khi thử tiếp.");
        var next = new ArrayList<>(entries);
        next.add(new Entry(owner, request.requestId(), fingerprint(request, payment.channel()), payment, request));
        if (file != null) write(next);
        entries = next;
    }
    private String fingerprint(CommerceDtos.Request request, String channel) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(json.writeValueAsBytes(List.of(channel, request))));
        } catch (Exception e) { throw unavailable(); }
    }
    private boolean isPaid(DemoCardPayment.View p) {
        return p != null && p.code() != null && p.reference() != null && p.paidAt() != null
                && p.amount() != null && p.amount().signum() >= 0 && p.challenge() == null
                && (("LOCAL".equals(p.channel()) && "PAID_DEMO".equals(p.status()))
                || ("VNPAY_SANDBOX".equals(p.channel()) && "PAID_SANDBOX".equals(p.status())));
    }
    private void load() {
        if (entries != null) return;
        try {
            if (Files.notExists(file)) { entries = new ArrayList<>(); return; }
            if (Files.size(file) > MAX_BYTES) throw unavailable();
            var saved = json.readValue(Files.readAllBytes(file), Snapshot.class);
            if (saved.version() != 1 || saved.entries() == null || saved.entries().size() > MAX_ENTRIES)
                throw unavailable();
            Set<String> codes = new HashSet<>(), requests = new HashSet<>(), references = new HashSet<>();
            for (var e : saved.entries()) {
                if (e == null || e.owner() == null || e.requestId() == null || e.fingerprint() == null
                        || !isPaid(e.payment()) || !codes.add(e.payment().code())
                        || !requests.add(e.owner() + ":" + e.requestId()) || !references.add(e.payment().reference()))
                    throw unavailable();
            }
            entries = new ArrayList<>(saved.entries());
        } catch (Exception e) { throw unavailable(); }
    }
    private void write(List<Entry> next) {
        Path temporary = null;
        try {
            byte[] bytes = json.writeValueAsBytes(new Snapshot(1, next));
            if (bytes.length > MAX_BYTES) throw unavailable();
            Files.createDirectories(file.getParent());
            temporary = Files.createTempFile(file.getParent(), "paid-orders-", ".tmp");
            try (var output = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                var buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) output.write(buffer);
                output.force(true);
            }
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } catch (Exception e) { throw unavailable(); }
        finally { if (temporary != null) try { Files.deleteIfExists(temporary); } catch (Exception ignored) { /* Keep the prior ledger. */ } }
    }
    private AppException unavailable() {
        return new AppException(HttpStatus.SERVICE_UNAVAILABLE,
                "Không đọc/ghi được kho đơn thanh toán thử. Chưa xác nhận thành công; vui lòng thử lại hoặc liên hệ quản trị.");
    }
}
