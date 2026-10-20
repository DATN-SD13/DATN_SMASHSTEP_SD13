package com.smashstep.datn.invoice.controller;

import com.smashstep.datn.common.response.ApiResponse;
import com.smashstep.datn.storefront.checkout.OnlineBankPayment;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hoa-don/{code}/thanh-toan-qr")
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
@RequiredArgsConstructor
public class OnlineQrPaymentController {
    private final OnlineBankPayment payment;
    @GetMapping public ApiResponse<OnlineBankPayment.Payment> details(@PathVariable String code) {
        return ApiResponse.ok(payment.staffDetails(code));
    }
    @PostMapping public ApiResponse<OnlineBankPayment.Payment> confirm(@PathVariable String code,
            @Valid @RequestBody OnlineBankPayment.Confirmation request) {
        return ApiResponse.ok(payment.confirm(code, request));
    }
}
