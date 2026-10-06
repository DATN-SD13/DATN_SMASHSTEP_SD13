package com.smashstep.datn.promotion;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.customer.entity.KhachHang;
import com.smashstep.datn.customer.repository.KhachHangRepository;
import com.smashstep.datn.promotion.entity.PhieuGiamGia;
import com.smashstep.datn.promotion.entity.PhieuGiamGiaKhachHang;
import com.smashstep.datn.promotion.repository.PhieuGiamGiaKhachHangRepository;
import com.smashstep.datn.promotion.repository.PhieuGiamGiaRepository;
import com.smashstep.datn.promotion.response.PhieuGiamGiaRequest;
import com.smashstep.datn.promotion.service.PhieuGiamGiaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PhieuGiamGiaServiceTest {
    @Test void zeroDiscountAndPercentOver100AreRejected() {
        var request = request(); request.setDiscountValue(BigDecimal.ZERO);
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(AppException.class, () -> service.create(request)).getStatus());
        request.setDiscountValue(new BigDecimal("101"));
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(AppException.class, () -> service.create(request)).getStatus());
        verify(vouchers, never()).save(any());
    }

    @Test void publicResponseUsesStoredFormDespiteActiveLeftoverAssignment() {
        var voucher = new PhieuGiamGia(); voucher.setId(2L); voucher.setHinhThucPhieu(1); voucher.setTrangThai(1);
        when(vouchers.findById(2L)).thenReturn(Optional.of(voucher));
        var link = new PhieuGiamGiaKhachHang(); link.setIdKhachHang(customers.findById(10L).orElseThrow()); link.setTrangThai(1); links.add(link);
        assertEquals(1, service.getById(2L).getForm());
        assertEquals(List.of(10L), service.getById(2L).getCustomerIds());
    }

    @Test void finiteUpdateCannotDropQuantityBelowUsageAndUnlimitedCan() {
        var voucher = new PhieuGiamGia(); voucher.setId(2L); voucher.setTrangThai(1); voucher.setSoLuongDaDung(20);
        when(vouchers.findByIdForUpdate(2L)).thenReturn(Optional.of(voucher));
        var request = request();
        assertThrows(AppException.class, () -> service.update(2L, request));
        request.setUnlimited(true); request.setQuantity(null);
        assertTrue(service.update(2L, request).getUnlimited()); assertNull(voucher.getSoLuong());
    }
    private final PhieuGiamGiaRepository vouchers = mock(PhieuGiamGiaRepository.class);
    private final PhieuGiamGiaKhachHangRepository assignments = mock(PhieuGiamGiaKhachHangRepository.class);
    private final KhachHangRepository customers = mock(KhachHangRepository.class);
    private final com.smashstep.datn.common.config.DatabaseCapabilities database = mock(com.smashstep.datn.common.config.DatabaseCapabilities.class);
    private final PhieuGiamGiaService service = new PhieuGiamGiaService(vouchers, assignments, customers, database);
    private final List<PhieuGiamGiaKhachHang> links = new ArrayList<>();

    @BeforeEach
    void setup() {
        when(database.hasColumn("phieu_giam_gia", "hinh_thuc_phieu")).thenReturn(true);
        when(vouchers.save(any())).thenAnswer(call -> {
            PhieuGiamGia voucher = call.getArgument(0);
            if (voucher.getId() == null) voucher.setId(2L);
            return voucher;
        });
        when(assignments.findByIdPhieuGiamGia_Id(any())).thenReturn(links);
        when(assignments.save(any())).thenAnswer(call -> {
            PhieuGiamGiaKhachHang link = call.getArgument(0);
            links.add(link);
            return link;
        });
        var customer = new KhachHang();
        customer.setId(10L);
        customer.setTrangThai(1);
        when(customers.findById(10L)).thenReturn(Optional.of(customer));
    }

    private PhieuGiamGiaRequest request() {
        var request = new PhieuGiamGiaRequest();
        request.setCode("PGG007");
        request.setName("Giảm giá demo");
        request.setForm(1);
        request.setDiscountType(1);
        request.setDiscountValue(BigDecimal.TEN);
        request.setQuantity(10);
        request.setStartDate("2026-10-01");
        request.setEndDate("2026-10-31");
        return request;
    }

    @Test
    void malformedDateIsBadRequestWithoutWriting() {
        var request = request();
        request.setStartDate("2026-02-30");
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(AppException.class, () -> service.create(request)).getStatus());
        verify(vouchers, never()).save(any());
    }

    @Test
    void duplicateCodeIsConflictWithoutWriting() {
        when(vouchers.existsByMaPhieuGiamGia("PGG007")).thenReturn(true);
        assertEquals(HttpStatus.CONFLICT, assertThrows(AppException.class, () -> service.create(request())).getStatus());
        verify(vouchers, never()).save(any());
    }

    @Test
    void privateVoucherRequiresRecipient() {
        var request = request();
        request.setForm(2);
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(AppException.class, () -> service.create(request)).getStatus());
        verify(vouchers, never()).save(any());
    }

    @Test
    void privateVoucherPersistsOneAssignmentForDuplicateRecipientIds() {
        var request = request();
        request.setForm(2);
        request.setCustomerIds(List.of(10L, 10L));
        var response = service.create(request);
        assertEquals(2, response.getForm());
        assertEquals(List.of(10L), response.getCustomerIds());
        assertEquals(1, links.size());
        verify(assignments, times(1)).save(any());
    }

    @Test
    void changingPrivateToPublicKeepsUsageHistoryAndDisablesAssignment() {
        var voucher = new PhieuGiamGia();
        voucher.setId(2L);
        voucher.setTrangThai(1);
        when(vouchers.findById(2L)).thenReturn(Optional.of(voucher));
        when(vouchers.findByIdForUpdate(2L)).thenReturn(Optional.of(voucher));
        var link = new PhieuGiamGiaKhachHang();
        link.setIdKhachHang(customers.findById(10L).orElseThrow());
        link.setIdPhieuGiamGia(voucher);
        link.setTrangThai(1);
        var used = java.time.LocalDateTime.of(2026, 10, 1, 12, 0);
        link.setNgaySuDung(used);
        links.add(link);
        var response = service.update(2L, request());
        assertEquals(1, response.getForm());
        assertEquals(0, link.getTrangThai());
        assertEquals(used, link.getNgaySuDung());
        verify(assignments, never()).delete(any(PhieuGiamGiaKhachHang.class));
    }

    @Test
    void unlimitedVoucherUsesNullQuantityWithoutExtraColumn() {
        var request = request();
        request.setUnlimited(true);
        request.setQuantity(null);
        var response = service.create(request);
        assertTrue(response.getUnlimited());
        assertNull(response.getQuantity());
    }

    @Test
    void removingVoucherIsSoftAndPreservesInvoiceReferences() {
        var voucher = new PhieuGiamGia();
        voucher.setId(2L);
        voucher.setTrangThai(1);
        when(vouchers.findById(2L)).thenReturn(Optional.of(voucher));
        when(vouchers.findByIdForUpdate(2L)).thenReturn(Optional.of(voucher));
        service.delete(2L);
        assertEquals(-1, voucher.getTrangThai());
        verify(vouchers, never()).delete(any(PhieuGiamGia.class));
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(AppException.class, () -> service.getById(2L)).getStatus());
    }

    @Test
    void allExistingVoucherMutationsAcquireTheSameLockUsedByCheckout() {
        var voucher = new PhieuGiamGia();
        voucher.setId(2L);
        voucher.setTrangThai(1);
        voucher.setSoLuongDaDung(3);
        when(vouchers.findByIdForUpdate(2L)).thenReturn(Optional.of(voucher));

        service.update(2L, request());
        service.deactivate(2L);
        service.activate(2L);
        service.delete(2L);

        assertEquals(3, voucher.getSoLuongDaDung());
        verify(vouchers, times(4)).findByIdForUpdate(2L);
        verify(vouchers, never()).findById(anyLong());
    }
}
