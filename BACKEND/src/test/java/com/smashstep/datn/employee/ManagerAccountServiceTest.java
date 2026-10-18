package com.smashstep.datn.employee;

import com.smashstep.datn.employee.dto.NhanVienRequest;
import com.smashstep.datn.employee.entity.NhanVien;
import com.smashstep.datn.employee.entity.VaiTro;
import com.smashstep.datn.employee.mail.EmployeeMailService;
import com.smashstep.datn.employee.repository.NhanVienRepository;
import com.smashstep.datn.employee.repository.VaiTroRepository;
import com.smashstep.datn.employee.service.NhanVienService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ManagerAccountServiceTest {
    private NhanVienRepository employees;
    private EmployeeMailService mail;
    private VaiTro role;
    private NhanVienService service;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach void setup() {
        employees = mock(NhanVienRepository.class);
        VaiTroRepository roles = mock(VaiTroRepository.class);
        mail = mock(EmployeeMailService.class);
        role = new VaiTro(); role.setId(1L); role.setTrangThai(1); role.setTenVaiTro("Quản lý");
        when(roles.findById(1L)).thenReturn(Optional.of(role));
        when(employees.saveAndFlush(any())).thenAnswer(call -> {
            NhanVien employee = call.getArgument(0); employee.setId(77L); return employee;
        });
        when(employees.save(any())).thenAnswer(call -> call.getArgument(0));
        service = new NhanVienService(employees, roles, mail, encoder);
    }
    private NhanVienRequest request() {
        NhanVienRequest r = new NhanVienRequest();
        r.setName("Quản lý kiểm thử"); r.setEmail("manager@example.test"); r.setPhone("0891234567");
        r.setRoleId(1L); r.setGender(1); r.setDob(LocalDate.of(2000, 1, 1));
        r.setProvince("Hà Nội"); r.setWard("Ba Đình"); return r;
    }
    @Test void managerPasswordIsBcryptAndEmailUsesGeneratedPassword() {
        var response = service.them(request());
        ArgumentCaptor<NhanVien> saved = ArgumentCaptor.forClass(NhanVien.class);
        verify(employees).save(saved.capture());
        ArgumentCaptor<String> password = ArgumentCaptor.forClass(String.class);
        verify(mail).sendManagerAccountEmail(eq("manager@example.test"), eq("Quản lý kiểm thử"), eq(response.getUsername()), password.capture());
        String temporary = password.getValue();
        assertEquals(10, temporary.length());
        assertTrue(temporary.matches(".*[A-Z].*")); assertTrue(temporary.matches(".*[a-z].*"));
        assertTrue(temporary.matches(".*[0-9].*")); assertTrue(temporary.matches(".*[@#$%].*"));
        assertNotEquals(temporary, saved.getValue().getMatKhau());
        assertTrue(encoder.matches(temporary, saved.getValue().getMatKhau()));
    }
    @Test void ordinaryEmployeeReceivesOwnCredentialsWithoutManagerEmail() {
        role.setTenVaiTro("Nhân viên"); var response = service.them(request());
        ArgumentCaptor<NhanVien> saved = ArgumentCaptor.forClass(NhanVien.class);
        verify(employees).save(saved.capture());
        ArgumentCaptor<String> password = ArgumentCaptor.forClass(String.class);
        verify(mail).sendEmployeeAccountEmail(eq("manager@example.test"), eq("Quản lý kiểm thử"),
                eq("Nhân viên"), eq(response.getUsername()), password.capture());
        String temporary = password.getValue();
        assertEquals(10, temporary.length());
        assertTrue(temporary.matches(".*[A-Z].*")); assertTrue(temporary.matches(".*[a-z].*"));
        assertTrue(temporary.matches(".*[0-9].*")); assertTrue(temporary.matches(".*[@#$%].*"));
        assertNotEquals(temporary, saved.getValue().getMatKhau());
        assertTrue(encoder.matches(temporary, saved.getValue().getMatKhau()));
        assertSame(role, saved.getValue().getIdVaiTro());
        verify(mail, never()).sendManagerAccountEmail(anyString(), anyString(), anyString(), anyString());
    }
    @Test void mailIsDeferredUntilSuccessfulTransactionCommit() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.them(request()); verifyNoInteractions(mail);
            var callbacks = TransactionSynchronizationManager.getSynchronizations(); assertEquals(1, callbacks.size());
            callbacks.get(0).afterCommit();
            verify(mail).sendManagerAccountEmail(anyString(), anyString(), anyString(), anyString());
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
    }
    @Test void failedSaveNeverSendsCredentials() {
        when(employees.save(any())).thenThrow(new IllegalStateException("save failed"));
        assertThrows(IllegalStateException.class, () -> service.them(request())); verifyNoInteractions(mail);
    }
    @Test void mailFailureDoesNotUndoCreatedAccount() {
        doThrow(new IllegalStateException("mail unavailable")).when(mail).sendManagerAccountEmail(anyString(), anyString(), anyString(), anyString());
        assertEquals("NV0077", service.them(request()).getCode()); verify(employees).save(any());
    }
}