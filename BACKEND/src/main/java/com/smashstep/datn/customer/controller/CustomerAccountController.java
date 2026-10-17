package com.smashstep.datn.customer.controller;

import com.smashstep.datn.auth.security.JwtAuthFilter.AuthDetails;
import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.common.response.ApiResponse;
import com.smashstep.datn.customer.dto.*;
import com.smashstep.datn.customer.service.CustomerAccountService;
import com.smashstep.datn.customer.service.KhachHangService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/account")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
public class CustomerAccountController {
    private final CustomerAccountService account;
    private final KhachHangService customers;

    @GetMapping
    public ApiResponse<KhachHangResponse> profile(Authentication auth) {
        return ApiResponse.ok(account.profile(customerId(auth)));
    }

    @PutMapping
    public ApiResponse<KhachHangResponse> update(Authentication auth, @Valid @RequestBody CustomerProfileRequest request) {
        return ApiResponse.ok("Đã cập nhật hồ sơ", account.update(customerId(auth), request));
    }

    @PutMapping("/password")
    public ApiResponse<Void> password(Authentication auth, @Valid @RequestBody CustomerPasswordRequest request) {
        account.changePassword(customerId(auth), request);
        return ApiResponse.<Void>success(null);
    }

    @PostMapping("/addresses")
    public ApiResponse<DiaChiKhachHangResponse> createAddress(Authentication auth, @Valid @RequestBody DiaChiKhachHangRequest request) {
        return ApiResponse.ok(customers.createAddress(account.ownCode(customerId(auth)), request));
    }

    @PutMapping("/addresses/{id}")
    public ApiResponse<DiaChiKhachHangResponse> updateAddress(Authentication auth, @PathVariable Long id, @Valid @RequestBody DiaChiKhachHangRequest request) {
        return ApiResponse.ok(customers.updateAddress(account.ownCode(customerId(auth)), id, request));
    }

    @PatchMapping("/addresses/{id}/default")
    public ApiResponse<DiaChiKhachHangResponse> defaultAddress(Authentication auth, @PathVariable Long id) {
        return ApiResponse.ok(customers.setDefaultAddress(account.ownCode(customerId(auth)), id));
    }

    private Long customerId(Authentication auth) {
        if (auth == null || !(auth.getDetails() instanceof AuthDetails details)
                || !"KHACH_HANG".equals(details.userType()) || details.userId() == null) {
            throw AppException.forbidden("Vui lòng đăng nhập bằng tài khoản khách hàng");
        }
        return details.userId();
    }
}
