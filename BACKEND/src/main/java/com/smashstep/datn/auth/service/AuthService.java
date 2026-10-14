package com.smashstep.datn.auth.service;

import com.smashstep.datn.auth.dto.LoginRequest;
import com.smashstep.datn.auth.dto.LoginResponse;
import com.smashstep.datn.auth.security.JwtUtil;
import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.customer.entity.KhachHang;
import com.smashstep.datn.customer.repository.KhachHangRepository;
import com.smashstep.datn.employee.entity.NhanVien;
import com.smashstep.datn.employee.repository.NhanVienRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final NhanVienRepository nhanVienRepository;
    private final KhachHangRepository khachHangRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(
            NhanVienRepository  nhanVienRepository,
            KhachHangRepository khachHangRepository,
            PasswordEncoder     passwordEncoder,
            JwtUtil             jwtUtil
    ) {
        this.nhanVienRepository  = nhanVienRepository;
        this.khachHangRepository = khachHangRepository;
        this.passwordEncoder     = passwordEncoder;
        this.jwtUtil             = jwtUtil;
    }

    /**
     * Đăng nhập chung — nếu có userType thì tìm đúng loại,
     * nếu không thì tự động nhận biết (nhân viên trước, khách hàng sau).
     */
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername().trim();
        String password = request.getPassword();
        String userType = request.getUserType(); // nullable

        if ("NHAN_VIEN".equals(userType)) {
            return loginNhanVien(
                nhanVienRepository.findByTenDangNhap(username)
                    .orElseThrow(() -> AppException.unauthorized("Tên đăng nhập hoặc mật khẩu không đúng")),
                password);
        }
        if ("KHACH_HANG".equals(userType)) {
            return loginKhachHang(
                khachHangRepository.findByTenTaiKhoanIgnoreCaseOrEmailIgnoreCase(username, username)
                    .orElseThrow(() -> AppException.unauthorized("Tên đăng nhập hoặc mật khẩu không đúng")),
                password);
        }

        // Không có userType — tự nhận biết
        var nvOpt = nhanVienRepository.findByTenDangNhap(username);
        if (nvOpt.isPresent()) return loginNhanVien(nvOpt.get(), password);

        var khOpt = khachHangRepository.findByTenTaiKhoanIgnoreCaseOrEmailIgnoreCase(username, username);
        if (khOpt.isPresent()) return loginKhachHang(khOpt.get(), password);

        throw AppException.unauthorized("Tên đăng nhập hoặc mật khẩu không đúng");
    }

    // ===== NHÂN VIÊN =====

    private LoginResponse loginNhanVien(NhanVien nv, String password) {
        if (nv.getMatKhau() == null) {
            throw AppException.unauthorized("Tài khoản chưa được kích hoạt. Vui lòng liên hệ quản trị viên.");
        }
        if (!passwordEncoder.matches(password, nv.getMatKhau())) {
            throw AppException.unauthorized("Tên đăng nhập hoặc mật khẩu không đúng");
        }
        if (!Integer.valueOf(1).equals(nv.getTrangThai())) {
            throw AppException.unauthorized("Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.");
        }

        String role  = resolveNhanVienRole(nv);
        String token = jwtUtil.generateToken(nv.getId(), nv.getTenDangNhap(), role, "NHAN_VIEN");

        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userType("NHAN_VIEN")
                .id(nv.getId())
                .code(nv.getMaNhanVien())
                .name(nv.getTenNhanVien())
                .email(nv.getEmail())
                .role(role)
                .avatar(nv.getHinhAnh())
                .build();
    }

    
    private String resolveNhanVienRole(NhanVien nv) {
        if (nv.getIdVaiTro() == null) return "STAFF";
        if (Long.valueOf(2L).equals(nv.getIdVaiTro().getId())) return "ADMIN";
        return "STAFF";
    }

    // ===== KHÁCH HÀNG =====

    private LoginResponse loginKhachHang(KhachHang kh, String password) {
        if (kh.getMatKhau() == null) {
            throw AppException.unauthorized("Tài khoản chưa có mật khẩu. Vui lòng đăng ký hoặc đặt lại mật khẩu.");
        }
        if (!passwordEncoder.matches(password, kh.getMatKhau())) {
            throw AppException.unauthorized("Tên đăng nhập hoặc mật khẩu không đúng");
        }
        if (!Integer.valueOf(1).equals(kh.getTrangThai())) {
            throw AppException.unauthorized("Tài khoản đã bị khóa.");
        }

        String token = jwtUtil.generateToken(kh.getId(), kh.getTenTaiKhoan(), "CUSTOMER", "KHACH_HANG");

        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userType("KHACH_HANG")
                .id(kh.getId())
                .code(kh.getMaKhachHang())
                .name(kh.getTenKhachHang())
                .email(kh.getEmail())
                .role("CUSTOMER")
                .avatar(kh.getHinhAnh())
                .build();
    }
}
