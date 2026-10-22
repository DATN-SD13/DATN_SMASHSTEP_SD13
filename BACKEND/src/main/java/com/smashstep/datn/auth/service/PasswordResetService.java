package com.smashstep.datn.auth.service;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.customer.entity.KhachHang;
import com.smashstep.datn.customer.repository.KhachHangRepository;
import com.smashstep.datn.employee.entity.NhanVien;
import com.smashstep.datn.employee.repository.NhanVienRepository;
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

/**
 * Expiring one-use reset links without introducing columns into the locked canonical schema.
 * Tokens are local to this application instance; restarting it invalidates outstanding links.
 * Supports both KhachHang (customer) and NhanVien (admin/staff) accounts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {
    private final KhachHangRepository khachHangRepository;
    private final NhanVienRepository nhanVienRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /** Base URL cho khách hàng (customer frontend, port 5174 theo cấu hình). */
    @Value("${smashstep.website-url:http://localhost:5174}")
    private String websiteUrl;

    /** Base URL cho admin/staff frontend (port 5173 theo cấu hình). */
    @Value("${app.frontend-url:http://localhost:5173}")
    private String adminFrontendUrl;

    private static final int TOKEN_EXPIRY_MINUTES = 30;
    private static final SecureRandom RANDOM = new SecureRandom();
    private final Map<String, Grant> grants = new ConcurrentHashMap<>();
    private Clock clock = Clock.systemUTC();

    /**
     * userType: "KHACH_HANG" hoặc "NHAN_VIEN" — dùng để chọn URL reset link đúng.
     */
    private record Grant(Long userId, String userType, String passwordSnapshot, Instant issuedAt, Instant expiresAt) {}

    // Keep the same response for unknown, locked, throttled and active email addresses.
    public synchronized void guiLinkDatLaiMatKhau(String email) {
        Instant now = clock.instant();
        grants.entrySet().removeIf(e -> !e.getValue().expiresAt().isAfter(now));

        // Tìm trong bảng NhanVien trước (ưu tiên admin/staff vì form admin gọi endpoint này)
        var nvResult = nhanVienRepository.findByEmailIgnoreCase(email.trim());
        if (nvResult.isPresent() && Integer.valueOf(1).equals(nvResult.get().getTrangThai())) {
            guiLinkChoNhanVien(nvResult.get(), now);
            return;
        }

        // Nếu không phải NhanVien, tìm KhachHang
        var khResult = khachHangRepository.findByEmailIgnoreCase(email.trim());
        if (khResult.isPresent() && Integer.valueOf(1).equals(khResult.get().getTrangThai())) {
            guiLinkChoKhachHang(khResult.get(), now);
        }
        // Không tìm thấy hoặc tài khoản bị khóa → im lặng (tránh user enumeration)
    }

    private void guiLinkChoNhanVien(NhanVien nv, Instant now) {
        Long userId = nv.getId();
        if (grants.values().stream().anyMatch(g -> g.userId().equals(userId)
                && "NHAN_VIEN".equals(g.userType()) && g.issuedAt().plusSeconds(60).isAfter(now))) return;
        if (grants.size() >= 10000) return;
        grants.entrySet().removeIf(e -> e.getValue().userId().equals(userId) && "NHAN_VIEN".equals(e.getValue().userType()));
        byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String key = digest(token);
        grants.put(key, new Grant(userId, "NHAN_VIEN", nv.getMatKhau(), now, now.plusSeconds(TOKEN_EXPIRY_MINUTES * 60L)));
        try {
            // Admin reset link trỏ về frontend admin (port 5173), route /dat-lai-mat-khau
            String link = adminFrontendUrl.replaceAll("/+$", "") + "/dat-lai-mat-khau?token=" + token;
            String tenHienThi = nv.getTenNhanVien() != null ? nv.getTenNhanVien() : nv.getTenDangNhap();
            emailService.sendPasswordResetEmail(nv.getEmail(), tenHienThi, link, TOKEN_EXPIRY_MINUTES);
        } catch (RuntimeException ex) {
            grants.remove(key);
            log.warn("Không thể gửi email đặt lại mật khẩu cho nhân viên; yêu cầu giữ phản hồi chung.");
        }
    }

    private void guiLinkChoKhachHang(KhachHang customer, Instant now) {
        Long userId = customer.getId();
        if (grants.values().stream().anyMatch(g -> g.userId().equals(userId)
                && "KHACH_HANG".equals(g.userType()) && g.issuedAt().plusSeconds(60).isAfter(now))) return;
        if (grants.size() >= 10000) return;
        grants.entrySet().removeIf(e -> e.getValue().userId().equals(userId) && "KHACH_HANG".equals(e.getValue().userType()));
        byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String key = digest(token);
        grants.put(key, new Grant(userId, "KHACH_HANG", customer.getMatKhau(), now, now.plusSeconds(TOKEN_EXPIRY_MINUTES * 60L)));
        try {
            String link = websiteUrl.replaceAll("/+$", "") + "/dat-lai-mat-khau?token=" + token;
            emailService.sendPasswordResetEmail(customer.getEmail(), customer.getTenKhachHang(), link, TOKEN_EXPIRY_MINUTES);
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
        if (grant == null || !grant.expiresAt().isAfter(clock.instant())) {
            grants.remove(key);
            throw AppException.badRequest("Link đặt lại mật khẩu không hợp lệ, đã hết hạn hoặc đã được sử dụng.");
        }

        if ("NHAN_VIEN".equals(grant.userType())) {
            NhanVien nv = nhanVienRepository.findById(grant.userId())
                .filter(n -> Integer.valueOf(1).equals(n.getTrangThai()) && Objects.equals(n.getMatKhau(), grant.passwordSnapshot()))
                .orElse(null);
            if (nv == null) {
                grants.remove(key);
                throw AppException.badRequest("Link đặt lại mật khẩu không hợp lệ, đã hết hạn hoặc đã được sử dụng.");
            }
            nv.setMatKhau(passwordEncoder.encode(matKhauMoi));
            nv.setNgayCapNhat(LocalDateTime.now(clock));
            nhanVienRepository.save(nv);
        } else {
            KhachHang customer = khachHangRepository.findById(grant.userId())
                .filter(c -> Integer.valueOf(1).equals(c.getTrangThai()) && Objects.equals(c.getMatKhau(), grant.passwordSnapshot()))
                .orElse(null);
            if (customer == null) {
                grants.remove(key);
                throw AppException.badRequest("Link đặt lại mật khẩu không hợp lệ, đã hết hạn hoặc đã được sử dụng.");
            }
            customer.setMatKhau(passwordEncoder.encode(matKhauMoi));
            customer.setNgayCapNhat(LocalDateTime.now(clock));
            khachHangRepository.save(customer);
        }
        grants.remove(key);
    }

    @Transactional(readOnly = true)
    public boolean kiemTraToken(String token) {
        if (token == null || token.isBlank() || token.length() > 128) return false;
        String key = digest(token);
        Grant grant = grants.get(key);
        if (grant == null || !grant.expiresAt().isAfter(clock.instant())) return false;

        if ("NHAN_VIEN".equals(grant.userType())) {
            return nhanVienRepository.findById(grant.userId())
                .filter(n -> Integer.valueOf(1).equals(n.getTrangThai()) && Objects.equals(n.getMatKhau(), grant.passwordSnapshot()))
                .isPresent();
        } else {
            return khachHangRepository.findById(grant.userId())
                .filter(c -> Integer.valueOf(1).equals(c.getTrangThai()) && Objects.equals(c.getMatKhau(), grant.passwordSnapshot()))
                .isPresent();
        }
    }

    private static String digest(String value) {
        if (value == null || value.isBlank() || value.length() > 128) return "";
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
