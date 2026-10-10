package com.smashstep.datn.promotion;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.customer.entity.KhachHang;
import com.smashstep.datn.customer.repository.KhachHangRepository;
import com.smashstep.datn.promotion.entity.PhieuGiamGia;
import com.smashstep.datn.promotion.entity.PhieuGiamGiaKhachHang;
import com.smashstep.datn.promotion.repository.PhieuGiamGiaKhachHangRepository;
import com.smashstep.datn.promotion.repository.PhieuGiamGiaRepository;
import com.smashstep.datn.promotion.response.PhieuGiamGiaRequest;
import com.smashstep.datn.promotion.service.EmailService;
import com.smashstep.datn.promotion.service.PhieuGiamGiaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
    EmailService emailService = Mockito.mock(EmailService.class);
    private final PhieuGiamGiaService service = new PhieuGiamGiaService(vouchers, assignments, customers, database, emailService);
    private final List<PhieuGiamGiaKhachHang> links = new ArrayList<>();

    @BeforeEach
    void setup() {
        links.clear();
        Mockito.reset(emailService);
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
        customer.setTenKhachHang("Khách hàng 10");
        customer.setEmail("kh10@example.com");
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
        request.setMinOrderValue(BigDecimal.ZERO);
        request.setMaxDiscount(new BigDecimal("200000"));
        request.setQuantity(10);
        request.setStartDate("2026-10-01T00:00:00");
        request.setEndDate("2026-10-31T00:00:00");
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
        request.setQuantity(1);
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

    @Test
    void updateTH1_onlyVoucherInfoChanged_sendsUpdatedEmailToRetainedCustomers() {
        var voucher = new PhieuGiamGia();
        voucher.setId(2L);
        voucher.setTenPhieuGiamGia("Phiếu 10%");
        voucher.setMaPhieuGiamGia("PGG01");
        voucher.setHinhThucPhieu(2);
        voucher.setLoaiGiamGia(1);
        voucher.setGiaTriGiam(BigDecimal.TEN);
        voucher.setGiaTriToiThieu(new BigDecimal("500000"));
        voucher.setGiamToiDa(new BigDecimal("200000"));
        var start = java.time.LocalDateTime.now().plusDays(2).withNano(0);
        var end = java.time.LocalDateTime.now().plusDays(7).withNano(0);
        voucher.setNgayBatDau(start);
        voucher.setNgayKetThuc(end);
        voucher.setTrangThai(1);

        when(vouchers.findByIdForUpdate(2L)).thenReturn(Optional.of(voucher));

        // Existing customer: 10L
        var link = new PhieuGiamGiaKhachHang();
        link.setIdPhieuGiamGia(voucher);
        link.setIdKhachHang(customers.findById(10L).orElseThrow());
        link.setTrangThai(1);
        links.add(link);

        // TH1: Customer kept (10L), Discount changed from 10% to 15%
        var request = request();
        request.setName("Phiếu 10%");
        request.setForm(2);
        request.setCustomerIds(List.of(10L));
        request.setQuantity(1);
        request.setDiscountValue(new BigDecimal("15"));
        request.setMinOrderValue(new BigDecimal("500000"));
        request.setMaxDiscount(new BigDecimal("200000"));
        request.setStartDate(start.toString());
        request.setEndDate(end.toString());

        service.update(2L, request);

        // Retained customer (10L) should receive Updated Email
        verify(emailService, timeout(1000).atLeastOnce()).sendVoucherUpdatedEmail(
                eq("kh10@example.com"), any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
        verify(emailService, never()).sendVoucherCancelledEmail(any(), any(), any(), any());
    }

    @Test
    void updateTH2_onlyCustomerListChanged_sendsNewMailToAddedAndCancelMailToRemoved() {
        var voucher = new PhieuGiamGia();
        voucher.setId(2L);
        voucher.setTenPhieuGiamGia("Phiếu 10%");
        voucher.setMaPhieuGiamGia("PGG01");
        voucher.setHinhThucPhieu(2);
        voucher.setLoaiGiamGia(1);
        voucher.setGiaTriGiam(BigDecimal.TEN);
        voucher.setGiaTriToiThieu(new BigDecimal("500000"));
        voucher.setGiamToiDa(new BigDecimal("200000"));
        var start = java.time.LocalDateTime.now().plusDays(2).withNano(0);
        var end = java.time.LocalDateTime.now().plusDays(7).withNano(0);
        voucher.setNgayBatDau(start);
        voucher.setNgayKetThuc(end);
        voucher.setTrangThai(1);

        when(vouchers.findByIdForUpdate(2L)).thenReturn(Optional.of(voucher));

        // Setup 2nd customer (11L)
        var customer11 = new KhachHang();
        customer11.setId(11L);
        customer11.setTenKhachHang("Khách hàng 11");
        customer11.setEmail("kh11@example.com");
        customer11.setTrangThai(1);
        when(customers.findById(11L)).thenReturn(Optional.of(customer11));

        // Old recipient: 10L
        var link = new PhieuGiamGiaKhachHang();
        link.setIdPhieuGiamGia(voucher);
        link.setIdKhachHang(customers.findById(10L).orElseThrow());
        link.setTrangThai(1);
        links.add(link);

        // TH2: Voucher conditions unchanged, replace 10L with 11L
        var request = request();
        request.setName("Phiếu 10%");
        request.setForm(2);
        request.setCustomerIds(List.of(11L));
        request.setQuantity(1);
        request.setDiscountValue(BigDecimal.TEN);
        request.setMinOrderValue(new BigDecimal("500000"));
        request.setMaxDiscount(new BigDecimal("200000"));
        request.setStartDate(start.toString());
        request.setEndDate(end.toString());

        service.update(2L, request);

        // 11L is newly added -> receives sendVoucherEmail (New voucher)
        verify(emailService, timeout(1000).atLeastOnce()).sendVoucherEmail(
                eq("kh11@example.com"), any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
        // 10L was removed -> receives sendVoucherCancelledEmail
        verify(emailService, timeout(1000).atLeastOnce()).sendVoucherCancelledEmail(
                eq("kh10@example.com"), any(), any(), any()
        );
        // No updated email should be sent because voucher info didn't change
        verify(emailService, never()).sendVoucherUpdatedEmail(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void updateTH3_bothInfoAndCustomersChanged_sendsAllThreeTypesCorrectly() {
        var voucher = new PhieuGiamGia();
        voucher.setId(2L);
        voucher.setTenPhieuGiamGia("Phiếu 10%");
        voucher.setMaPhieuGiamGia("PGG01");
        voucher.setHinhThucPhieu(2);
        voucher.setLoaiGiamGia(1);
        voucher.setGiaTriGiam(BigDecimal.TEN);
        voucher.setGiaTriToiThieu(new BigDecimal("500000"));
        voucher.setGiamToiDa(new BigDecimal("200000"));
        var start = java.time.LocalDateTime.now().plusDays(2).withNano(0);
        var end = java.time.LocalDateTime.now().plusDays(7).withNano(0);
        voucher.setNgayBatDau(start);
        voucher.setNgayKetThuc(end);
        voucher.setTrangThai(1);

        when(vouchers.findByIdForUpdate(2L)).thenReturn(Optional.of(voucher));

        // Setup customers: 10L (old & kept), 11L (old & removed), 12L (newly added)
        var customer11 = new KhachHang();
        customer11.setId(11L);
        customer11.setTenKhachHang("Khách hàng 11");
        customer11.setEmail("kh11@example.com");
        customer11.setTrangThai(1);
        when(customers.findById(11L)).thenReturn(Optional.of(customer11));

        var customer12 = new KhachHang();
        customer12.setId(12L);
        customer12.setTenKhachHang("Khách hàng 12");
        customer12.setEmail("kh12@example.com");
        customer12.setTrangThai(1);
        when(customers.findById(12L)).thenReturn(Optional.of(customer12));

        // Old recipients: 10L, 11L
        var link10 = new PhieuGiamGiaKhachHang();
        link10.setIdPhieuGiamGia(voucher);
        link10.setIdKhachHang(customers.findById(10L).orElseThrow());
        link10.setTrangThai(1);
        links.add(link10);

        var link11 = new PhieuGiamGiaKhachHang();
        link11.setIdPhieuGiamGia(voucher);
        link11.setIdKhachHang(customer11);
        link11.setTrangThai(1);
        links.add(link11);

        // TH3: Change discount to 15% AND change customer list to {10L, 12L} (10L kept, 11L removed, 12L added)
        var request = request();
        request.setName("Phiếu 15%");
        request.setForm(2);
        request.setCustomerIds(List.of(10L, 12L));
        request.setQuantity(2);
        request.setDiscountValue(new BigDecimal("15"));
        request.setMinOrderValue(new BigDecimal("500000"));
        request.setMaxDiscount(new BigDecimal("200000"));
        request.setStartDate(start.toString());
        request.setEndDate(end.toString());

        service.update(2L, request);

        // 12L is newly added -> receives sendVoucherEmail (New)
        verify(emailService, timeout(1000).atLeastOnce()).sendVoucherEmail(
                eq("kh12@example.com"), any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
        // 11L is removed -> receives sendVoucherCancelledEmail (Cancel)
        verify(emailService, timeout(1000).atLeastOnce()).sendVoucherCancelledEmail(
                eq("kh11@example.com"), any(), any(), any()
        );
        // 10L is retained -> receives sendVoucherUpdatedEmail (Update)
        verify(emailService, timeout(1000).atLeastOnce()).sendVoucherUpdatedEmail(
                eq("kh10@example.com"), any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }
}
