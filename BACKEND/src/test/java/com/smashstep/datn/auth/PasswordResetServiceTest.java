package com.smashstep.datn.auth;

import com.smashstep.datn.auth.service.PasswordResetService;
import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.customer.entity.KhachHang;
import com.smashstep.datn.customer.repository.KhachHangRepository;
import com.smashstep.datn.promotion.service.EmailService;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class PasswordResetServiceTest {
    final KhachHangRepository customers=mock(KhachHangRepository.class);
    final PasswordEncoder encoder=mock(PasswordEncoder.class);
    final EmailService email=mock(EmailService.class);
    final PasswordResetService service=new PasswordResetService(customers,encoder,email);
    final KhachHang customer=new KhachHang();
    final Instant now=Instant.parse("2026-10-16T00:00:00Z");
    @BeforeEach void setup(){
        customer.setId(7L); customer.setEmail("customer@example.test"); customer.setTenKhachHang("Khách kiểm thử"); customer.setTrangThai(1);customer.setMatKhau("old-hash");
        when(customers.findByEmailIgnoreCase(customer.getEmail())).thenReturn(Optional.of(customer));
        when(customers.findById(7L)).thenReturn(Optional.of(customer));
        when(encoder.encode(anyString())).thenReturn("new-hash");
        ReflectionTestUtils.setField(service,"websiteUrl","http://localhost:5173");
        ReflectionTestUtils.setField(service,"clock",Clock.fixed(now,ZoneOffset.UTC));
    }
    String issue(){
        service.guiLinkDatLaiMatKhau(customer.getEmail());
        var link=ArgumentCaptor.forClass(String.class);
        verify(email,atLeastOnce()).sendPasswordResetEmail(eq(customer.getEmail()),eq(customer.getTenKhachHang()),link.capture(),eq(30));
        return link.getValue().substring(link.getValue().indexOf("token=")+6);
    }
    @Test void tokenStorageDoesNotWriteCustomerAndTokenIsOneUse(){
        String token=issue(); verify(customers,never()).save(any());
        assertTrue(service.kiemTraToken(token));
        service.datLaiMatKhau(token,"new-password");
        verify(customers).save(customer); assertEquals("new-hash",customer.getMatKhau());
        assertFalse(service.kiemTraToken(token));
        assertThrows(AppException.class,()->service.datLaiMatKhau(token,"another-password"));
        verify(customers,times(1)).save(any());
    }
    @Test void expiredTokensCannotResetPassword(){
        String token=issue(); ReflectionTestUtils.setField(service,"clock",Clock.fixed(now.plusSeconds(1801),ZoneOffset.UTC));
        assertFalse(service.kiemTraToken(token)); assertThrows(AppException.class,()->service.datLaiMatKhau(token,"new-password"));
        verify(customers,never()).save(any());
    }
    @Test void unknownAndLockedAddressesHaveSameNoOpBehavior(){
        service.guiLinkDatLaiMatKhau("unknown@example.test"); customer.setTrangThai(0); service.guiLinkDatLaiMatKhau(customer.getEmail());
        verifyNoInteractions(email,encoder); verify(customers,never()).save(any());
    }
    @Test void cooldownPreventsRepeatedEmails(){String token=issue();service.guiLinkDatLaiMatKhau(customer.getEmail());verify(email,times(1)).sendPasswordResetEmail(anyString(),anyString(),anyString(),anyInt());assertTrue(service.kiemTraToken(token));}
    @Test void aNewRequestInvalidatesOlderLinks(){
        String first=issue();ReflectionTestUtils.setField(service,"clock",Clock.fixed(now.plusSeconds(61),ZoneOffset.UTC));String second=issue();
        assertFalse(service.kiemTraToken(first));assertTrue(service.kiemTraToken(second));
    }
    @Test void accountLockAndPasswordChangesInvalidateOutstandingLinks(){
        String token=issue();customer.setTrangThai(0);assertFalse(service.kiemTraToken(token));customer.setTrangThai(1);customer.setMatKhau("changed");assertFalse(service.kiemTraToken(token));
    }
    @Test void failedEmailDoesNotLeaveAnUsableGrantOrLeakAccountExistence(){
        doThrow(new IllegalStateException("mail unavailable")).when(email).sendPasswordResetEmail(anyString(),anyString(),anyString(),anyInt());
        assertDoesNotThrow(()->service.guiLinkDatLaiMatKhau(customer.getEmail()));verify(customers,never()).save(any());
    }
    @Test void malformedTokensAndOversizedPasswordAreRejected(){
        assertFalse(service.kiemTraToken("x".repeat(129)));assertFalse(service.kiemTraToken(null));String token=issue();
        assertThrows(AppException.class,()->service.datLaiMatKhau(token,"x"));
        assertThrows(AppException.class,()->service.datLaiMatKhau(token,"あ".repeat(30)));verify(customers,never()).save(any());
    }
}
