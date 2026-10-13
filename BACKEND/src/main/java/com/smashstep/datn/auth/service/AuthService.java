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

    public LoginResponse login(LoginRequest request) {
        String userType = request.getUserType();

        return switch (userType) {
            case "NHAN_VIEN"  -> loginNhanVien(request);
            case "KHACH_HANG" -> loginKhachHang(request);
            default -> throw AppException.badRequest("Loại người dùng không hợp lệ");
        };
    }

    // ===== NHÂN VIÊN (ADMIN / STAFF) =====

    private LoginResponse loginNhanVien(LoginRequest request) {
        NhanVien nv = nhanVienRepository
                .findByTenDangNhap(request.getUsername().trim())
                .orElseThrow(() -> AppException.unauthorized("Tên đăng nhập hoặc mật khẩu không đúng"));

        if (nv.getMatKhau() == null) {
            throw AppException.unauthorized("Tài khoản chưa được kích hoạt. Vui lòng liên hệ quản trị viên.");
        }

        if (!passwordEncoder.matches(request.getPassword(), nv.getMatKhau())) {
            throw AppException.unauthorized("Tên đăng nhập hoặc mật khẩu không đúng");
        }

        if (!Integer.valueOf(1).equals(nv.getTrangThai())) {
            throw AppException.unauthorized("Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.");
        }

        String role = resolveNhanVienRole(nv);

        String token = jwtUtil.generateToken(
                nv.getId(),
                nv.getTenDangNhap(),
                role,
                "NHAN_VIEN"
        );

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

    /**
     * Ánh xạ tên vai trò DB → role string chuẩn.
     * DB seed có: "Quản lý" (id=1), "Nhân viên" (id=2).
     * Mở rộng ở đây nếu thêm vai trò mới.
     */
    private String resolveNhanVienRole(NhanVien nv) {
        if (nv.getIdVaiTro() == null) return "STAFF";
        String tenVaiTro = nv.getIdVaiTro().getTenVaiTro();
        if (tenVaiTro == null) return "STAFF";
        return switch (tenVaiTro.trim().toLowerCase()) {
            case "quản lý", "quan ly", "admin" -> "ADMIN";
            default -> "STAFF";
        };
    }

    // ===== KHÁCH HÀNG =====

    private LoginResponse loginKhachHang(LoginRequest request) {
        String username = request.getUsername().trim();

        // Cho phép đăng nhập bằng ten_tai_khoan hoặc email
        KhachHang kh = khachHangRepository
                .findByTenTaiKhoanIgnoreCaseOrEmailIgnoreCase(username, username)
                .orElseThrow(() -> AppException.unauthorized("Tên đăng nhập hoặc mật khẩu không đúng"));

        if (kh.getMatKhau() == null) {
            throw AppException.unauthorized("Tài khoản chưa có mật khẩu. Vui lòng đăng ký hoặc đặt lại mật khẩu.");
        }

        if (!passwordEncoder.matches(request.getPassword(), kh.getMatKhau())) {
            throw AppException.unauthorized("Tên đăng nhập hoặc mật khẩu không đúng");
        }

        if (!Integer.valueOf(1).equals(kh.getTrangThai())) {
            throw AppException.unauthorized("Tài khoản đã bị khóa.");
        }

        String token = jwtUtil.generateToken(
                kh.getId(),
                kh.getTenTaiKhoan(),
                "CUSTOMER",
                "KHACH_HANG"
        );

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
