package com.smashstep.datn.sales;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.customer.entity.KhachHang;
import com.smashstep.datn.employee.entity.NhanVien;
import com.smashstep.datn.payment.entity.PhuongThucThanhToan;
import com.smashstep.datn.product.entity.SanPham;
import com.smashstep.datn.product.entity.SanPhamChiTiet;
import com.smashstep.datn.promotion.entity.PhieuGiamGia;
import com.smashstep.datn.promotion.entity.PhieuGiamGiaKhachHang;
import com.smashstep.datn.sales.dto.SalesRequest;
import com.smashstep.datn.sales.service.SalesService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class SalesVoucherRulesTest {
    private final EntityManager em = mock(EntityManager.class);
    private final SalesService service = new SalesService(em, new com.smashstep.datn.common.pricing.CurrentPriceService(em), new com.smashstep.datn.invoice.service.InvoiceCodeService(em));
    private final PhieuGiamGia voucher = new PhieuGiamGia();
    private final PhieuGiamGiaKhachHang link = new PhieuGiamGiaKhachHang();
    private SalesRequest request;
    @BeforeEach void setup() {
        var employee = new NhanVien(); employee.setTrangThai(1); when(em.find(NhanVien.class, 1L)).thenReturn(employee);
        var customer = new KhachHang(); customer.setId(2L); customer.setTrangThai(1); when(em.find(KhachHang.class, 2L)).thenReturn(customer);
        var payment = new PhuongThucThanhToan(); payment.setTrangThai(1); payment.setMaPhuongThuc("TIEN_MAT"); when(em.find(PhuongThucThanhToan.class, 1L)).thenReturn(payment);
        var product = new SanPham(); product.setTrangThai(1);
        var variant = new SanPhamChiTiet(); variant.setId(3L); variant.setIdSanPham(product); variant.setTrangThai(1);
        variant.setKichHoat(true); variant.setSoLuong(5); variant.setGiaBan(new BigDecimal("1000")); when(em.find(SanPhamChiTiet.class, 3L)).thenReturn(variant);
        TypedQuery<com.smashstep.datn.promotion.entity.ChiTietDotGiamGia> campaign = mock(TypedQuery.class, RETURNS_SELF); when(campaign.getResultList()).thenReturn(List.of()); when(em.createQuery(anyString(), eq(com.smashstep.datn.promotion.entity.ChiTietDotGiamGia.class))).thenReturn(campaign);
        voucher.setId(4L); voucher.setTrangThai(1); voucher.setHinhThucPhieu(1); voucher.setLoaiGiamGia(1); voucher.setGiaTriGiam(BigDecimal.TEN);
        voucher.setSoLuong(null); voucher.setSoLuongDaDung(100); voucher.setNgayBatDau(LocalDateTime.now().minusDays(1)); voucher.setNgayKetThuc(LocalDateTime.now().plusDays(1));
        TypedQuery<PhieuGiamGia> vouchers = mock(TypedQuery.class, RETURNS_SELF); when(vouchers.getResultList()).thenReturn(List.of(voucher)); when(em.createQuery(anyString(), eq(PhieuGiamGia.class))).thenReturn(vouchers);
        link.setIdKhachHang(customer); link.setTrangThai(1);
        TypedQuery<PhieuGiamGiaKhachHang> assignments = mock(TypedQuery.class, RETURNS_SELF); when(assignments.getResultList()).thenReturn(List.of(link)); when(em.createQuery(anyString(), eq(PhieuGiamGiaKhachHang.class))).thenReturn(assignments);
        request = new SalesRequest(); request.setRequestId(UUID.randomUUID().toString()); request.setEmployeeId(1L); request.setPaymentMethodId(1L); request.setVoucherCode("TEST");
        var item = new SalesRequest.Item(); item.setVariantId(3L); item.setQuantity(1); item.setUnitPrice(new BigDecimal("1000")); request.setItems(List.of(item));
    }
    @Test void unlimitedPublicVoucherIgnoresLeftoverAssignmentAndNeedsNoCustomer() {
        link.setNgaySuDung(LocalDateTime.now());
        assertEquals(0, new BigDecimal("100").compareTo(service.quote(request).discount()));
    }
    @Test void privateVoucherRequiresMatchingUnusedCustomer() {
        voucher.setHinhThucPhieu(2);
        assertThrows(AppException.class, () -> service.quote(request));
        request.setCustomerId(2L); assertEquals(0, new BigDecimal("100").compareTo(service.quote(request).discount()));
        link.setNgaySuDung(LocalDateTime.now()); assertThrows(AppException.class, () -> service.quote(request));
    }
    @Test void finiteVoucherExhaustionAndInvalidDiscountsAreRejected() {
        voucher.setSoLuong(100); assertThrows(AppException.class, () -> service.quote(request));
        voucher.setSoLuong(null); voucher.setGiaTriGiam(BigDecimal.ZERO); assertThrows(AppException.class, () -> service.quote(request));
        voucher.setGiaTriGiam(new BigDecimal("101")); assertThrows(AppException.class, () -> service.quote(request));
    }
    @Test void checkoutRequiresPaidAmountEvenBeforeLockOrZeroTotalEvaluation() {
        assertThrows(AppException.class, () -> service.checkout(request));
        verify(em, never()).unwrap(any());
    }
    @Test void fullyDiscountedQuoteCanTotalZeroButCheckoutStillNeedsExplicitAmount() {
        voucher.setGiaTriGiam(new BigDecimal("100"));
        assertEquals(0, service.quote(request).total().signum());
        assertThrows(AppException.class, () -> service.checkout(request));
    }
}
