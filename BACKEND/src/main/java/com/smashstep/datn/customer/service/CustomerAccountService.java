package com.smashstep.datn.customer.service;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.customer.dto.*;
import com.smashstep.datn.customer.entity.KhachHang;
import com.smashstep.datn.customer.repository.KhachHangRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerAccountService {
    private final KhachHangRepository customers;
    private final KhachHangService customerService;
    private final PasswordEncoder passwordEncoder;

    public String ownCode(Long userId) {
        return activeCustomer(userId).getMaKhachHang();
    }

    public KhachHangResponse profile(Long userId) {
        return customerService.getByCode(ownCode(userId));
    }

    @Transactional
    public KhachHangResponse update(Long userId, CustomerProfileRequest request) {
        KhachHang customer = activeCustomer(userId);
        if (request.getDob() != null && request.getDob().isAfter(LocalDate.now().minusYears(15))) {
            throw AppException.badRequest("Khách hàng phải đủ 15 tuổi trở lên");
        }
        String phone = request.getPhone() == null || request.getPhone().isBlank() ? null : request.getPhone().trim();
        if (phone != null && !phone.equals(customer.getSoDienThoai())
                && customers.existsBySoDienThoaiAndIdNot(phone, userId)) {
            throw AppException.conflict("Số điện thoại đã được sử dụng bởi khách hàng khác");
        }
        // Keep login identity, email, avatar and account status unchanged.
        customer.setTenKhachHang(request.getName().trim());
        customer.setSoDienThoai(phone);
        customer.setGioiTinh(request.getGender());
        customer.setNgaySinh(request.getDob());
        customer.setNgayCapNhat(LocalDateTime.now());
        customers.save(customer);
        return customerService.getByCode(customer.getMaKhachHang());
    }

    @Transactional
    public void changePassword(Long userId, CustomerPasswordRequest request) {
        KhachHang customer = activeCustomer(userId);
        if (customer.getMatKhau() == null || !passwordEncoder.matches(request.getCurrentPassword(), customer.getMatKhau())) {
            throw AppException.badRequest("Mật khẩu hiện tại không đúng");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw AppException.badRequest("Mật khẩu xác nhận không khớp");
        }
        if (request.getNewPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw AppException.badRequest("Mật khẩu mới tối đa 72 byte UTF-8");
        }
        if (passwordEncoder.matches(request.getNewPassword(), customer.getMatKhau())) {
            throw AppException.badRequest("Mật khẩu mới phải khác mật khẩu hiện tại");
        }
        customer.setMatKhau(passwordEncoder.encode(request.getNewPassword()));
        customer.setNgayCapNhat(LocalDateTime.now());
        customers.save(customer);
    }

    private KhachHang activeCustomer(Long userId) {
        KhachHang customer = customers.findById(userId)
                .orElseThrow(() -> AppException.unauthorized("Tài khoản không còn tồn tại. Vui lòng đăng nhập lại."));
        if (!Integer.valueOf(1).equals(customer.getTrangThai())) {
            throw AppException.unauthorized("Tài khoản đã bị khóa. Vui lòng liên hệ cửa hàng.");
        }
        return customer;
    }
}
