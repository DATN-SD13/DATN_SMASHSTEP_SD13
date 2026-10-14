package com.smashstep.datn.sales.controller;

import com.smashstep.datn.common.response.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Keep method-security denials out of the application's generic 500 handler. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = SalesController.class)
public class SalesExceptionHandler {
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> forbidden(AccessDeniedException ignored) {
        return ResponseEntity.status(403).body(ApiResponse.error("Chỉ ADMIN/STAFF được sử dụng POS"));
    }
}
