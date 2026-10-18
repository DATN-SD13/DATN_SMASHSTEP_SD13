package com.smashstep.datn.auth.service;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.customer.entity.KhachHang;
import com.smashstep.datn.customer.repository.KhachHangRepository;
import com.smashstep.datn.promotion.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Expiring one-use reset links without introducing columns into the locked canonical schema.
 * Tokens are local to this application instance; restarting it invalidates outstanding links. */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {
    private final KhachHangRepository khachHangRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    @Value("${smashstep.website-url:http://localhost:5173}")
    private String websiteUrl;
    private static final int TOKEN_EXPIRY_MINUTES = 30;
    private static final SecureRandom RANDOM = new SecureRandom();
    private final Map<String, Grant> grants = new ConcurrentHashMap<>();
    private Clock clock = Clock.systemUTC();
    private record Grant(Long customerId, String passwordSnapshot, Instant issuedAt, Instant expiresAt) {}

    // Keep the same response for unknown, locked, throttled and active email addresses.
    public synchronized void guiLinkDatLaiMatKhau(String email) {
        Instant now = clock.instant();
        grants.entrySet().removeIf(e -> !e.getValue().expiresAt().isAfter(now));
        var result = khachHangRepository.findByEmailIgnoreCase(email.trim());
        if (result.isEmpty() || !Integer.valueOf(1).equals(result.get().getTrangThai())) return;
        KhachHang customer = result.get();
        if (grants.values().stream().anyMatch(g -> g.customerId().equals(customer.getId()) && g.issuedAt().plusSeconds(60).isAfter(now))) return;
        if (grants.size() >= 10000) return;
        grants.entrySet().removeIf(e -> e.getValue().customerId().equals(customer.getId()));
        byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String key = digest(token);
        grants.put(key,new Grant(customer.getId(),customer.getMatKhau(),now,now.plusSeconds(TOKEN_EXPIRY_MINUTES*60L)));
        try {
            String link = websiteUrl.replaceAll("/+$", "") + "/khach-hang/dat-lai-mat-khau?token=" + token;
            emailService.sendPasswordResetEmail(customer.getEmail(),customer.getTenKhachHang(),link,TOKEN_EXPIRY_MINUTES);
        } catch (RuntimeException ex) {
            grants.remove(key);
            log.warn("Không thể gửi email đặt lại mật khẩu; yêu cầu giữ phản hồi chung.");
        }
    }

    @Transactional
    public synchronized void datLaiMatKhau(String token, String matKhauMoi) {
        if (matKhauMoi == null || matKhauMoi.length() < 6 || matKhauMoi.getBytes(StandardCharsets.UTF_8).length > 72)
            throw AppException.badRequest("Mật khẩu phải có ít nhất 6 ký tự và tối đa 72 byte UTF-8.");
        String key = digest(token);
        Grant grant = grants.get(key);
        KhachHang customer = validCustomer(grant);
        if (customer == null) {
            grants.remove(key);
            throw AppException.badRequest("Link đặt lại mật khẩu không hợp lệ, đã hết hạn hoặc đã được sử dụng.");
        }
        customer.setMatKhau(passwordEncoder.encode(matKhauMoi));
        customer.setNgayCapNhat(LocalDateTime.now(clock));
        khachHangRepository.save(customer);
        grants.remove(key);
    }

    @Transactional(readOnly=true)
    public boolean kiemTraToken(String token) {
        if (token == null || token.isBlank() || token.length() > 128) return false;
        return validCustomer(grants.get(digest(token))) != null;
    }

    private KhachHang validCustomer(Grant grant) {
        if (grant == null || !grant.expiresAt().isAfter(clock.instant())) return null;
        return khachHangRepository.findById(grant.customerId())
            .filter(c -> Integer.valueOf(1).equals(c.getTrangThai()) && Objects.equals(c.getMatKhau(),grant.passwordSnapshot()))
            .orElse(null);
    }
    private static String digest(String value) {
        if (value == null || value.isBlank() || value.length() > 128) return "";
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
