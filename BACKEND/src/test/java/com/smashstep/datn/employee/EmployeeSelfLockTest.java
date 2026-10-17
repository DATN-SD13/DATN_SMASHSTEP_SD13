package com.smashstep.datn.employee;

import com.smashstep.datn.auth.security.JwtAuthFilter.AuthDetails;
import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.employee.dto.TrangThaiNhanVienRequest;
import com.smashstep.datn.employee.entity.NhanVien;
import com.smashstep.datn.employee.repository.NhanVienRepository;
import com.smashstep.datn.employee.service.NhanVienService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeSelfLockTest {
    @Mock NhanVienRepository employees;
    @InjectMocks NhanVienService service;
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    @Test void renamedLoginStillCannotLockOwnEmployeeIdentity(){
        var employee=new NhanVien();employee.setId(33L);employee.setMaNhanVien("NV0033");employee.setTrangThai(1);
        when(employees.findByMaNhanVien("NV0033")).thenReturn(Optional.of(employee));
        var auth=new UsernamePasswordAuthenticationToken("different-login@example.test",null,List.of());auth.setDetails(new AuthDetails(33L,"ADMIN","NHAN_VIEN"));
        SecurityContextHolder.getContext().setAuthentication(auth);
        var request=new TrangThaiNhanVienRequest();request.setStatus(0);
        assertThrows(AppException.class,()->service.capNhatTrangThai("NV0033",request));verify(employees,never()).save(any());
    }
}
