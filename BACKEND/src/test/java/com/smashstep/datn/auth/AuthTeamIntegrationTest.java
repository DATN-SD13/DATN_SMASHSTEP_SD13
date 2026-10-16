package com.smashstep.datn.auth;

import com.smashstep.datn.auth.dto.LoginRequest;
import com.smashstep.datn.auth.security.JwtUtil;
import com.smashstep.datn.auth.service.AuthService;
import com.smashstep.datn.customer.entity.KhachHang;
import com.smashstep.datn.customer.repository.KhachHangRepository;
import com.smashstep.datn.employee.entity.*;
import com.smashstep.datn.employee.repository.NhanVienRepository;
import org.junit.jupiter.api.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthTeamIntegrationTest {
    final NhanVienRepository employees=mock(NhanVienRepository.class);
    final KhachHangRepository customers=mock(KhachHangRepository.class);
    final PasswordEncoder encoder=mock(PasswordEncoder.class);
    final AuthService service=new AuthService(employees,customers,encoder,mock(JwtUtil.class));
    LoginRequest request(String type){var r=new LoginRequest();r.setUsername("account@example.test");r.setPassword("fixture-password");r.setUserType(type);return r;}
    NhanVien employee(Long roleId,String roleName){
        var role=new VaiTro();role.setId(roleId);role.setTenVaiTro(roleName);
        var n=new NhanVien();n.setId(1L);n.setIdVaiTro(role);n.setTrangThai(1);n.setMatKhau("hash");n.setTenDangNhap("NV001");
        when(encoder.matches("fixture-password","hash")).thenReturn(true);
        when(employees.findByEmailIgnoreCase("account@example.test")).thenReturn(Optional.of(n));return n;
    }
    @Test void managerRoleUsesActualNameInsteadOfAssumingIdentityTwo(){employee(3L,"Quản lý");assertEquals("ADMIN",service.login(request("NHAN_VIEN")).getRole());}
    @Test void employeeWithIdentityTwoIsNotAccidentallyAdministrator(){employee(2L,"Nhân viên");assertEquals("STAFF",service.login(request("NHAN_VIEN")).getRole());}
    @Test void explicitCustomerTypeDoesNotFallBackToEmployee(){
        var customer=new KhachHang();customer.setId(9L);customer.setTrangThai(1);customer.setMatKhau("hash");customer.setTenTaiKhoan("customer");
        when(customers.findByTenTaiKhoanIgnoreCaseOrEmailIgnoreCase(anyString(),anyString())).thenReturn(Optional.of(customer));
        when(encoder.matches("fixture-password","hash")).thenReturn(true);
        assertEquals("CUSTOMER",service.login(request("KHACH_HANG")).getRole());verifyNoInteractions(employees);
    }
    @Test void nullRoleNeverGrantsAdministrator(){var n=employee(2L,"Quản lý");n.setIdVaiTro(null);assertEquals("STAFF",service.login(request("NHAN_VIEN")).getRole());}
}
