
package com.smashstep.datn.auth.service;

import com.smashstep.datn.auth.dto.LoginRequest;
import com.smashstep.datn.auth.dto.LoginResponse;
import com.smashstep.datn.auth.dto.RegisterRequest;
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

    // ===== ĐĂNG KÝ KHÁCH HÀNG =====

    @org.springframework.transaction.annotation.Transactional
    public LoginResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase(java.util.Locale.ROOT);

        if (khachHangRepository.existsByEmailIgnoreCase(email)) {
            throw AppException.conflict("Email này đã được sử dụng.");
        }
        String phone = request.getPhone() == null || request.getPhone().isBlank() ? null : request.getPhone().trim();
        if (phone != null && khachHangRepository.existsBySoDienThoai(phone)) {
            throw AppException.conflict("Số điện thoại này đã được sử dụng.");
        }

        KhachHang kh = new KhachHang();
        kh.setMaKhachHang(sinhMaKhachHang());
        kh.setTenKhachHang(request.getName().trim());
        kh.setEmail(email);
        kh.setSoDienThoai(phone);
        // Dùng phần trước @ của email làm tên tài khoản, đảm bảo không trùng
        String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "");
        if (baseUsername.isBlank()) baseUsername = "khachhang";
        if (baseUsername.length() > 54) baseUsername = baseUsername.substring(0, 54);
        String username = baseUsername;
        int suffix = 1;
        while (khachHangRepository.existsByTenTaiKhoan(username)) {
            username = baseUsername + suffix++;
        }
        kh.setTenTaiKhoan(username);
        kh.setMatKhau(passwordEncoder.encode(request.getPassword()));
        kh.setTrangThai(1);
        kh.setNgayTao(java.time.LocalDateTime.now());
        kh.setNgayCapNhat(java.time.LocalDateTime.now());

        kh = khachHangRepository.save(kh);

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

    private String sinhMaKhachHang() {
        String ma;
        do {
            ma = "KH" + String.format("%06d", (int)(Math.random() * 1_000_000));
        } while (khachHangRepository.existsByMaKhachHang(ma));
        return ma;
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
