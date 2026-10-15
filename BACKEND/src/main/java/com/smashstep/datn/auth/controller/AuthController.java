package com.smashstep.datn.auth.controller;

import com.smashstep.datn.auth.dto.ForgotPasswordRequest;
import com.smashstep.datn.auth.dto.LoginRequest;
import com.smashstep.datn.auth.dto.LoginResponse;
import com.smashstep.datn.auth.dto.ResetPasswordRequest;
import com.smashstep.datn.auth.security.JwtAuthFilter;
import com.smashstep.datn.auth.service.AuthService;
import com.smashstep.datn.auth.service.PasswordResetService;
import com.smashstep.datn.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    /**
     * Đăng nhập chung cho nhân viên và khách hàng.
     * Body: { username, password, userType: "NHAN_VIEN"|"KHACH_HANG" }
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Đăng xuất — xóa context server-side.
     * Client phải tự xóa token khỏi localStorage.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(ApiResponse.<Void>success(null));
    }

    /**
     * Thông tin người dùng đang đăng nhập.
     * GET /api/auth/me   Authorization: Bearer {token}
     *
     * Dùng cho: POS lấy thông tin nhân viên đang ca, các module kiểm tra quyền.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.<Map<String, Object>>error("Chưa đăng nhập"));
        }

        JwtAuthFilter.AuthDetails details = null;
        if (auth.getDetails() instanceof JwtAuthFilter.AuthDetails d) {
            details = d;
        }

        Map<String, Object> info = Map.of(
                "username", auth.getName(),
                "role",     details != null ? details.role()     : "",
                "userType", details != null ? details.userType() : "",
                "userId",   details != null ? details.userId()   : 0L
        );

        return ResponseEntity.ok(ApiResponse.success(info));
    }

    /**
     * Gửi link đặt lại mật khẩu về email.
     * POST /api/auth/quen-mat-khau
     * Body: { email }
     */
    @PostMapping("/quen-mat-khau")
    public ResponseEntity<ApiResponse<Void>> quenMatKhau(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        passwordResetService.guiLinkDatLaiMatKhau(request.getEmail());
        // Luôn trả thành công để tránh user enumeration
        return ResponseEntity.ok(ApiResponse.<Void>success(null));
    }

    /**
     * Kiểm tra token còn hợp lệ không (frontend dùng trước khi hiển thị form).
     * GET /api/auth/kiem-tra-token?token=xxx
     */
    @GetMapping("/kiem-tra-token")
    public ResponseEntity<ApiResponse<Boolean>> kiemTraToken(
            @RequestParam String token
    ) {
        boolean hopLe = passwordResetService.kiemTraToken(token);
        return ResponseEntity.ok(ApiResponse.success(hopLe));
    }

    /**
     * Đặt lại mật khẩu bằng token từ email.
     * POST /api/auth/dat-lai-mat-khau
     * Body: { token, newPassword }
     */
    @PostMapping("/dat-lai-mat-khau")
    public ResponseEntity<ApiResponse<Void>> datLaiMatKhau(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        passwordResetService.datLaiMatKhau(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.<Void>success(null));
    }
}
