package com.smashstep.datn.auth.service;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.customer.entity.KhachHang;
import com.smashstep.datn.customer.repository.KhachHangRepository;
import com.smashstep.datn.promotion.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final KhachHangRepository khachHangRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${smashstep.website-url:http://localhost:5173}")
    private String websiteUrl;

    /** Thời hạn token: 30 phút */
    private static final int TOKEN_EXPIRY_MINUTES = 30;

    /**
     * Bước 1: Nhận email, tạo token, gửi link reset về mail.
     * Không tiết lộ email có tồn tại hay không (luôn trả về thông báo thành công)
     * để tránh user enumeration.
     */
    @Transactional
    public void guiLinkDatLaiMatKhau(String email) {
        var khOpt = khachHangRepository.findByEmailIgnoreCase(email.trim());

        // Tìm không thấy → không làm gì, vẫn trả thành công (tránh user enumeration)
        if (khOpt.isEmpty()) return;

        KhachHang kh = khOpt.get();

        // Tài khoản bị khóa → từ chối
        if (!Integer.valueOf(1).equals(kh.getTrangThai())) {
            throw AppException.unauthorized("Tài khoản đã bị khóa. Vui lòng liên hệ hỗ trợ.");
        }

        // Tạo token ngẫu nhiên
        String token = UUID.randomUUID().toString();
        kh.setResetToken(token);
        kh.setResetTokenExpiry(LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES));
        khachHangRepository.save(kh);

        // Gửi email
        String resetLink = websiteUrl + "/khach-hang/dat-lai-mat-khau?token=" + token;
        String tenKhachHang = kh.getTenKhachHang() != null ? kh.getTenKhachHang() : kh.getEmail();
        emailService.sendPasswordResetEmail(kh.getEmail(), tenKhachHang, resetLink, TOKEN_EXPIRY_MINUTES);
    }

    /**
     * Bước 2: Xác minh token và đặt mật khẩu mới.
     */
    @Transactional
    public void datLaiMatKhau(String token, String matKhauMoi) {
        KhachHang kh = khachHangRepository.findByResetToken(token)
                .orElseThrow(() -> AppException.badRequest("Link đặt lại mật khẩu không hợp lệ hoặc đã được sử dụng."));

        if (kh.getResetTokenExpiry() == null ||
                LocalDateTime.now().isAfter(kh.getResetTokenExpiry())) {
            // Xóa token hết hạn
            kh.setResetToken(null);
            kh.setResetTokenExpiry(null);
            khachHangRepository.save(kh);
            throw AppException.badRequest("Link đặt lại mật khẩu đã hết hạn. Vui lòng yêu cầu lại.");
        }

        // Đặt mật khẩu mới và xóa token
        kh.setMatKhau(passwordEncoder.encode(matKhauMoi));
        kh.setResetToken(null);
        kh.setResetTokenExpiry(null);
        kh.setNgayCapNhat(LocalDateTime.now());
        khachHangRepository.save(kh);
    }

    /**
     * Kiểm tra token có hợp lệ không (dùng cho frontend validate trước khi hiển thị form).
     */
    public boolean kiemTraToken(String token) {
        return khachHangRepository.findByResetToken(token)
                .map(kh -> kh.getResetTokenExpiry() != null &&
                           LocalDateTime.now().isBefore(kh.getResetTokenExpiry()))
                .orElse(false);
    }
}
