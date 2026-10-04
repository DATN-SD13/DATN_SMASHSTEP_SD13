package com.smashstep.datn.common.exception;

import com.smashstep.datn.product.controller.SanPhamController;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(basePackageClasses = SanPhamController.class)
public class GlobalExceptionHandler {
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<?> uploadTooLarge(Exception ex) {
        return ResponseEntity.status(413).body(Map.of("message", "Ảnh không được vượt quá 5 MB."));
    }

    @ExceptionHandler({org.springframework.web.multipart.MultipartException.class,
            org.springframework.web.multipart.support.MissingServletRequestPartException.class})
    public ResponseEntity<?> invalidUpload(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("message", "Tệp ảnh không hợp lệ hoặc chưa được chọn."));
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<?> unsupportedMedia(Exception ex) {
        return ResponseEntity.status(415).body(Map.of("message", "Định dạng nội dung không được hỗ trợ."));
    }
    @ExceptionHandler(AppException.class)
    public ResponseEntity<?> business(AppException ex) {
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

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> unsupportedMethod(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(405).headers(ex.getHeaders())
                .body(Map.of("message", "API không hỗ trợ phương thức HTTP này"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> unexpected(Exception ex) {
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class).error("Product request failed", ex);
        return ResponseEntity.internalServerError().body(Map.of("message", "Không thể xử lý yêu cầu. Vui lòng thử lại."));
    }
}
