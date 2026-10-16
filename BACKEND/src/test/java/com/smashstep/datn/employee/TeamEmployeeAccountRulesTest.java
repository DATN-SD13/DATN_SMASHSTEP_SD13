package com.smashstep.datn.employee;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.employee.dto.NhanVienRequest;
import com.smashstep.datn.employee.entity.NhanVien;
import com.smashstep.datn.employee.entity.VaiTro;
import com.smashstep.datn.employee.mail.EmployeeMailService;
import com.smashstep.datn.employee.repository.NhanVienRepository;
import com.smashstep.datn.employee.repository.VaiTroRepository;
import com.smashstep.datn.employee.service.NhanVienService;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TeamEmployeeAccountRulesTest {
    final NhanVienRepository employees=mock(NhanVienRepository.class);
    final VaiTroRepository roles=mock(VaiTroRepository.class);
    final EmployeeMailService mail=mock(EmployeeMailService.class);
    final BCryptPasswordEncoder encoder=new BCryptPasswordEncoder();
    final NhanVienService service=new NhanVienService(employees,roles,mail,encoder);
    @BeforeEach void setup(){
        var role=new VaiTro();role.setId(2L);role.setTenVaiTro("Nhân viên");role.setTrangThai(1);
        when(roles.findById(2L)).thenReturn(Optional.of(role));
        when(employees.saveAndFlush(any())).thenAnswer(c->{NhanVien n=c.getArgument(0);n.setId(88L);return n;});
        when(employees.save(any())).thenAnswer(c->c.getArgument(0));
    }
    NhanVienRequest request(LocalDate dob){
        var r=new NhanVienRequest();r.setName("Nhân viên kiểm thử");r.setEmail("staff@example.test");
        r.setPhone("0891234567");r.setRoleId(2L);r.setGender(1);r.setDob(dob);return r;
    }
    @Test void exactEighteenthBirthdayIsAcceptedWithoutExposingCredentials(){
        var response=service.them(request(LocalDate.now().minusYears(18)));
        assertEquals("NV0088",response.getUsername());assertEquals(2L,response.getRoleId());
        var json=JsonMapper.builder().build().valueToTree(response);
        assertFalse(json.has("matKhau"));assertFalse(json.has("password"));
        verify(mail).sendEmployeeAccountEmail(eq("staff@example.test"),anyString(),eq("Nhân viên"),eq("NV0088"),anyString());
    }
    @Test void employeeYoungerThanEighteenIsRejectedBeforeAnySaveOrEmail(){
        var ex=assertThrows(AppException.class,()->service.them(request(LocalDate.now().minusYears(18).plusDays(1))));
        assertEquals(HttpStatus.BAD_REQUEST,ex.getStatus());assertTrue(ex.getMessage().contains("18"));
        verifyNoInteractions(employees,roles,mail);
    }
    @Test void futureBirthDateIsRejectedBeforeAnySaveOrEmail(){
        var ex=assertThrows(AppException.class,()->service.them(request(LocalDate.now().plusDays(1))));
        assertEquals(HttpStatus.BAD_REQUEST,ex.getStatus());verifyNoInteractions(employees,roles,mail);
    }
    @Test void ordinaryEmployeeEmailIsAlsoDeferredUntilCommit(){
        TransactionSynchronizationManager.initSynchronization();
        try{
            service.them(request(LocalDate.of(2000,1,1)));verifyNoInteractions(mail);
            var callbacks=TransactionSynchronizationManager.getSynchronizations();assertEquals(1,callbacks.size());
            callbacks.get(0).afterCommit();
            verify(mail).sendEmployeeAccountEmail(anyString(),anyString(),eq("Nhân viên"),eq("NV0088"),anyString());
        }finally{TransactionSynchronizationManager.clearSynchronization();}
    }
    @Test void failedStaffMailAfterCommitDoesNotUndoSavedAccount(){
        doThrow(new IllegalStateException("mock transport unavailable")).when(mail).sendEmployeeAccountEmail(anyString(),anyString(),anyString(),anyString(),anyString());
        TransactionSynchronizationManager.initSynchronization();
        try{
            assertEquals("NV0088",service.them(request(LocalDate.of(2000,1,1))).getUsername());
            assertDoesNotThrow(()->TransactionSynchronizationManager.getSynchronizations().get(0).afterCommit());
            verify(employees).save(any());
        }finally{TransactionSynchronizationManager.clearSynchronization();}
    }
}
