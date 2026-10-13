package com.smashstep.datn.auth.controller;

import com.smashstep.datn.auth.dto.LoginRequest;
import com.smashstep.datn.auth.dto.LoginResponse;
import com.smashstep.datn.auth.security.JwtAuthFilter;
import com.smashstep.datn.auth.service.AuthService;
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

    public AuthController(AuthService authService) {
        this.authService = authService;
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
}
