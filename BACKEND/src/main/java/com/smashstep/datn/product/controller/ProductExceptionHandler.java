package com.smashstep.datn.product.controller;

import com.smashstep.datn.product.service.ProductException;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(basePackageClasses = ProductController.class)
public class ProductExceptionHandler {
    @ExceptionHandler(ProductException.class)
    public ResponseEntity<?> business(ProductException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> fields.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("message", "Dữ liệu không hợp lệ. Hãy kiểm tra các trường nhập.", "errors", fields));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<?> invalid(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("message", "Dữ liệu không đúng định dạng hoặc thiếu nội dung bắt buộc"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> integrity(DataIntegrityViolationException ex) {
        return ResponseEntity.status(409).body(Map.of("message", "Dữ liệu xung đột với dữ liệu hiện có. Hãy tải lại và kiểm tra thông tin."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> unexpected(Exception ex) {
        org.slf4j.LoggerFactory.getLogger(ProductExceptionHandler.class).error("Product request failed", ex);
        return ResponseEntity.internalServerError().body(Map.of("message", "Không thể xử lý yêu cầu. Vui lòng thử lại."));
    }
}

