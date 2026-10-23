package com.smashstep.datn.storefront.checkout;

import com.smashstep.datn.auth.security.JwtAuthFilter.AuthDetails;
import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/customer/shop/demo-payments")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
public class DemoCardPaymentController {
    private final DemoCardPayment demo;
    @GetMapping("/config") public ApiResponse<DemoCardPayment.Config> config() { return ApiResponse.ok(demo.config()); }
    @PostMapping("/quote") public ApiResponse<CommerceDtos.Quote> quote(Authentication auth,@Valid @RequestBody CommerceDtos.Request request) { return ApiResponse.ok(demo.quote(id(auth),request)); }
    @PostMapping public ApiResponse<DemoCardPayment.View> create(Authentication auth,@Valid @RequestBody CommerceDtos.Request request) { return ApiResponse.ok(demo.create(id(auth),request)); }
    @GetMapping("/{code}") public ApiResponse<DemoCardPayment.View> get(Authentication auth,@PathVariable String code) { return ApiResponse.ok(demo.get(id(auth),code)); }
    @PostMapping("/{code}/account") public ApiResponse<DemoCardPayment.View> account(Authentication auth,@PathVariable String code,@Valid @RequestBody DemoCardPayment.Account account) { return ApiResponse.ok(demo.account(id(auth),code,account)); }
    @PostMapping("/{code}/card") public ApiResponse<DemoCardPayment.View> card(Authentication auth,@PathVariable String code,@Valid @RequestBody DemoCardPayment.Card card) { return ApiResponse.ok(demo.card(id(auth),code,card)); }
    @PostMapping("/{code}/otp") public ApiResponse<DemoCardPayment.View> otp(Authentication auth,@PathVariable String code,@Valid @RequestBody DemoCardPayment.Otp otp) { return ApiResponse.ok(demo.otp(id(auth),code,otp)); }
    @PostMapping("/{code}/cancel") public ApiResponse<DemoCardPayment.View> cancel(Authentication auth,@PathVariable String code) { return ApiResponse.ok(demo.cancel(id(auth),code)); }
    private Long id(Authentication auth) {
        if(auth==null || !(auth.getDetails() instanceof AuthDetails d) || !"KHACH_HANG".equals(d.userType()) || d.userId()==null)
            throw AppException.forbidden("Vui lòng đăng nhập bằng tài khoản khách hàng");
        return d.userId();
    }

    @RestController
    @RequiredArgsConstructor
    public static class AdminDemoOrders {
        private final DemoCardPayment demo;
        @GetMapping("/api/hoa-don/paid-demo-orders")
        @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
        public ApiResponse<List<DemoCardPayment.View>> orders() { return ApiResponse.ok(demo.paidOrders()); }
    }
}
