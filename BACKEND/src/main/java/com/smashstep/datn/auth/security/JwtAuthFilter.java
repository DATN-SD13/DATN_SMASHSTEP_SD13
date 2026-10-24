package com.smashstep.datn.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final NhanVienSessionService employeeSessions;

    public JwtAuthFilter(JwtUtil jwtUtil, NhanVienSessionService employeeSessions) {
        this.jwtUtil = jwtUtil;
        this.employeeSessions = employeeSessions;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain         chain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (jwtUtil.isValid(token)) {
                String username = jwtUtil.getUsername(token);
                String role     = jwtUtil.getRole(token);
                String userType = jwtUtil.getUserType(token);
                Long   userId   = jwtUtil.getUserId(token);

                if ("NHAN_VIEN".equals(userType) && !"/api/auth/login".equals(request.getServletPath())) {
                    NhanVienSessionService.Session current;
                    try { current = employeeSessions.current(userId); }
                    catch (RuntimeException unavailable) {
                        SecurityContextHolder.clearContext();
                        response.setStatus(503); response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"success\":false,\"message\":\"Chưa kiểm tra được quyền tài khoản. Vui lòng thử lại.\",\"data\":null}");
                        return;
                    }
                    if (current == null) {
                        SecurityContextHolder.clearContext();
                        response.setStatus(401); response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"success\":false,\"message\":\"Tài khoản đã bị khóa, bị xóa hoặc vai trò đã ngừng hoạt động. Vui lòng đăng nhập lại.\",\"data\":null}");
                        return;
                    }
                    role = current.role();
                    if (current.username() != null && !current.username().isBlank()) username = current.username();
                }
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

                var auth = new UsernamePasswordAuthenticationToken(username, null, authorities);

                // Lưu thêm thông tin để các service khác truy xuất
                auth.setDetails(new AuthDetails(userId, role, userType));

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        chain.doFilter(request, response);
    }

    /** Chi tiết bổ sung lưu trong Authentication.details */
    public record AuthDetails(Long userId, String role, String userType) {}
}
