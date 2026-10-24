package com.smashstep.datn.auth.security;

import com.smashstep.datn.employee.repository.NhanVienRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

/** JWT cung cấp danh tính; trạng thái và quyền nhân viên phải lấy lại từ DB. */
@Service
@RequiredArgsConstructor
public class NhanVienSessionService {
    private final NhanVienRepository employees;
    public record Session(String username, String role) {}
    @Transactional(readOnly = true)
    public Session current(Long id) {
        if (id == null) return null;
        var employee = employees.findById(id).orElse(null);
        if (employee == null || !Integer.valueOf(1).equals(employee.getTrangThai())) return null;
        var role = employee.getIdVaiTro();
        if (role != null && role.getTrangThai() != null && !Integer.valueOf(1).equals(role.getTrangThai())) return null;
        String name = role == null ? "" : role.getTenVaiTro();
        String normalized = Normalizer.normalize(name == null ? "" : name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ','d').replace('Đ','D').trim().toLowerCase(Locale.ROOT);
        String authority = Set.of("admin", "quan ly", "quan tri", "quan tri vien", "manager").contains(normalized) ? "ADMIN" : "STAFF";
        return new Session(employee.getTenDangNhap(), authority);
    }
}
