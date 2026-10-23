package com.smashstep.datn.storefront.checkout;

import com.smashstep.datn.auth.security.JwtAuthFilter.AuthDetails;
import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/customer/shop/vnpay-sandbox")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
public class VnpaySandboxController {
    private final VnpaySandboxPayment gateway;
    private final VnpaySandboxReconciliation reconciliation;
    @GetMapping("/config") public ApiResponse<VnpaySandboxPayment.Config> config() { return ApiResponse.ok(gateway.config()); }
    @PostMapping("/quote") public ApiResponse<CommerceDtos.Quote> quote(Authentication auth,@Valid @RequestBody CommerceDtos.Request body) { return ApiResponse.ok(gateway.quote(id(auth),body)); }
    @PostMapping public ApiResponse<VnpaySandboxPayment.Start> create(Authentication auth,@Valid @RequestBody CommerceDtos.Request body,HttpServletRequest request) { return ApiResponse.ok(gateway.create(id(auth),body,request.getRemoteAddr())); }
    @GetMapping("/{code}") public ApiResponse<DemoCardPayment.View> get(Authentication auth,@PathVariable String code) { return ApiResponse.ok(gateway.owned(id(auth),code)); }
    @PostMapping("/{code}/reconcile") public ApiResponse<VnpaySandboxPayment.Result> reconcile(Authentication auth,@PathVariable String code) { return ApiResponse.ok(reconciliation.owned(id(auth),code)); }
    private Long id(Authentication auth) {
        if(auth==null || !(auth.getDetails() instanceof AuthDetails details) || !"KHACH_HANG".equals(details.userType()) || details.userId()==null) throw AppException.forbidden("Vui lòng đăng nhập tài khoản khách hàng");
        return details.userId();
    }
    @RestController
    @RequestMapping("/api/payments/vnpay")
    @RequiredArgsConstructor
    public static class Callback {
        private final VnpaySandboxPayment gateway;
        private final VnpaySandboxReconciliation reconciliation;
        @GetMapping("/ipn") public Map<String,String> ipn(HttpServletRequest request) { return gateway.ipn(request.getParameterMap()); }
        @PostMapping("/reconcile-return") public ApiResponse<VnpaySandboxPayment.Result> reconcile(@RequestBody Map<String,String[]> proof) { return ApiResponse.ok(reconciliation.returned(proof)); }
        @GetMapping("/return-check") public ApiResponse<VnpaySandboxPayment.Result> returned(HttpServletRequest request) { return ApiResponse.ok(gateway.returned(request.getParameterMap())); }
    }
}
