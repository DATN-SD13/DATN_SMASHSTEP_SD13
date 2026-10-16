
package com.smashstep.datn.auth.service;

import com.smashstep.datn.auth.dto.LoginRequest;
import com.smashstep.datn.auth.dto.LoginResponse;
import com.smashstep.datn.auth.security.JwtUtil;
import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.customer.entity.KhachHang;
import com.smashstep.datn.customer.repository.KhachHangRepository;
import com.smashstep.datn.employee.entity.NhanVien;
import com.smashstep.datn.employee.repository.NhanVienRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final NhanVienRepository nhanVienRepository;
    private final KhachHangRepository khachHangRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(
            NhanVienRepository nhanVienRepository,
            KhachHangRepository khachHangRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil
    ) {
        this.nhanVienRepository = nhanVienRepository;
        this.khachHangRepository = khachHangRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Đăng nhập bằng email hoặc tên đăng nhập.
     * Nếu có userType thì tìm đúng loại tài khoản.
     * Nếu không có userType thì tự nhận diện nhân viên hoặc khách hàng.
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername() == null
                ? ""
                : request.getUsername().trim();

        String password = request.getPassword();
        String userType = request.getUserType();

        if (username.isEmpty() || password == null || password.isEmpty()) {
            throw AppException.unauthorized(
                    "Vui lòng nhập email/tên đăng nhập và mật khẩu"
            );
        }

        // Đăng nhập nhân viên bằng tên đăng nhập hoặc email
        if ("NHAN_VIEN".equals(userType)) {
            NhanVien nv = nhanVienRepository.findByTenDangNhap(username)
                    .or(() -> nhanVienRepository.findByEmailIgnoreCase(username))
                    .orElseThrow(() -> AppException.unauthorized(
                            "Tên đăng nhập/email hoặc mật khẩu không đúng"
                    ));

            return loginNhanVien(nv, password);
        }

        // Đăng nhập khách hàng bằng tên tài khoản hoặc email
        if ("KHACH_HANG".equals(userType)) {
            KhachHang kh = khachHangRepository
                    .findByTenTaiKhoanIgnoreCaseOrEmailIgnoreCase(
                            username, username
                    )
                    .orElseThrow(() -> AppException.unauthorized(
                            "Tên đăng nhập/email hoặc mật khẩu không đúng"
                    ));

            return loginKhachHang(kh, password);
        }

        // Không có userType: tự nhận diện nhân viên trước
        var nvOpt = nhanVienRepository.findByTenDangNhap(username)
                .or(() -> nhanVienRepository.findByEmailIgnoreCase(username));

        if (nvOpt.isPresent()) {
            return loginNhanVien(nvOpt.get(), password);
        }

        // Nếu không phải nhân viên, tìm khách hàng
        var khOpt = khachHangRepository
                .findByEmailIgnoreCase(username)
                .or(() -> khachHangRepository
                        .findByTenTaiKhoanIgnoreCaseOrEmailIgnoreCase(
                                username, username));

        KhachHang kh = khOpt.orElseThrow(() ->
                new AppException( HttpStatus.UNAUTHORIZED,"Tên đăng nhập/email hoặc mật khẩu không đúng"));

        return loginKhachHang(kh, password);

    }

    // ===== NHÂN VIÊN =====

    private LoginResponse loginNhanVien(
            NhanVien nv,
            String password
    ) {
        if (nv.getMatKhau() == null) {
            throw AppException.unauthorized(
                    "Tài khoản chưa được kích hoạt. Vui lòng liên hệ quản trị viên."
            );
        }

        if (!passwordEncoder.matches(password, nv.getMatKhau())) {
            throw AppException.unauthorized(
                    "Tên đăng nhập/email hoặc mật khẩu không đúng"
            );
        }

        if (!Integer.valueOf(1).equals(nv.getTrangThai())) {
            throw AppException.unauthorized(
                    "Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên."
            );
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

    private String resolveNhanVienRole(NhanVien nv) {
        if (nv.getIdVaiTro() == null) return "STAFF";
        String name = nv.getIdVaiTro().getTenVaiTro();
        if (name == null || name.isBlank()) return "STAFF";
        String normalized = java.text.Normalizer.normalize(name,java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ','d').replace('Đ','D').trim().toLowerCase(java.util.Locale.ROOT);
        return java.util.Set.of("admin","quan ly","quan tri","quan tri vien","manager").contains(normalized) ? "ADMIN" : "STAFF";
    }

    // ===== KHÁCH HÀNG =====

    private LoginResponse loginKhachHang(
            KhachHang kh,
            String password
    ) {
        if (kh.getMatKhau() == null) {
            throw AppException.unauthorized(
                    "Tài khoản chưa có mật khẩu. Vui lòng đăng ký hoặc đặt lại mật khẩu."
            );
        }

        if (!passwordEncoder.matches(password, kh.getMatKhau())) {
            throw AppException.unauthorized(
                    "Tên đăng nhập/email hoặc mật khẩu không đúng"
            );
        }

        if (!Integer.valueOf(1).equals(kh.getTrangThai())) {
            throw AppException.unauthorized(
                    "Tài khoản đã bị khóa."
            );
        }

        String token = jwtUtil.generateToken(
                kh.getId(),
                kh.getTenTaiKhoan() != null
                        ? kh.getTenTaiKhoan()
                        : kh.getEmail(),
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
