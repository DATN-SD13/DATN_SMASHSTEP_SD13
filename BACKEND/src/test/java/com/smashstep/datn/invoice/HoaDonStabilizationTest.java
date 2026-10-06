package com.smashstep.datn.invoice;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.employee.repository.NhanVienRepository;
import com.smashstep.datn.invoice.dto.*;
import com.smashstep.datn.invoice.entity.HoaDon;
import com.smashstep.datn.invoice.entity.LichSuHoaDon;
import com.smashstep.datn.invoice.entity.LichSuThanhToan;
import com.smashstep.datn.invoice.repository.*;
import com.smashstep.datn.invoice.service.HoaDonService;
import com.smashstep.datn.payment.entity.PhuongThucThanhToan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HoaDonStabilizationTest {
    private final HoaDonRepository invoices = mock(HoaDonRepository.class);
    private final HoaDonChiTietRepository items = mock(HoaDonChiTietRepository.class);
    private final LichSuHoaDonRepository histories = mock(LichSuHoaDonRepository.class);
    private final LichSuThanhToanRepository payments = mock(LichSuThanhToanRepository.class);
    private final NhanVienRepository employees = mock(NhanVienRepository.class);
    private final HoaDonService service = new HoaDonService(invoices, items, histories, payments, employees);

    private HoaDon invoice(int status) {
        HoaDon invoice = new HoaDon();
        invoice.setId(42L);
        invoice.setMaHoaDon("HD042");
        invoice.setTrangThai(status);
        when(invoices.findByMaHoaDonForUpdate("HD042")).thenReturn(Optional.of(invoice));
        when(invoices.findByMaHoaDon("HD042")).thenReturn(Optional.of(invoice));
        when(items.findByIdHoaDonIdOrderByIdAsc(42L)).thenReturn(List.of());
        when(histories.findByIdHoaDonIdOrderByNgayTaoDesc(42L)).thenReturn(List.of());
        when(payments.findByIdHoaDonIdOrderByThoiGianDesc(42L)).thenReturn(List.of());
        return invoice;
    }

    private CapNhatTrangThaiHoaDonDto request(int status) {
        var request = new CapNhatTrangThaiHoaDonDto();
        request.setTrangThai(status);
        request.setGhiChu("Xác nhận đơn hàng");
        return request;
    }

    @Test
    void writesNextStatusUnderInvoiceLockAndRecordsHistory() {
        HoaDon invoice = invoice(0);
        HoaDonDetailDto detail = service.capNhatTrangThai(" HD042 ", request(1));
        assertEquals(1, detail.getMaTrangThai());
        assertEquals(1, invoice.getTrangThai());
        verify(invoices).findByMaHoaDonForUpdate("HD042");
        verify(invoices).save(invoice);
        var captured = ArgumentCaptor.forClass(LichSuHoaDon.class);
        verify(histories).save(captured.capture());
        assertSame(invoice, captured.getValue().getIdHoaDon());
        assertEquals(1, captured.getValue().getTrangThai());
        assertEquals("Xác nhận đơn hàng", captured.getValue().getGhiChu());
        assertNotNull(captured.getValue().getNgayTao());
    }

    @Test
    void rejectsSkippedStatusAndMissingRequestWithoutWriting() {
        invoice(0);
        assertThrows(AppException.class, () -> service.capNhatTrangThai("HD042", request(2)));
        assertThrows(AppException.class, () -> service.capNhatTrangThai("HD042", null));
        verify(invoices, never()).save(any());
        verify(histories, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(ints = { 5, 6, 7 })
    void terminalInvoiceCannotCancelOrAdvance(int currentStatus) {
        invoice(currentStatus);
        assertThrows(AppException.class, () -> service.capNhatTrangThai("HD042", request(6)));
        assertThrows(AppException.class, () -> service.capNhatTrangThai("HD042", request(1)));
        verify(invoices, never()).save(any());
        verify(histories, never()).save(any());
    }

    @Test
    void waitingInvoiceCanCancelWithoutProductMutation() {
        invoice(8);
        assertEquals(6, service.capNhatTrangThai("HD042", request(6)).getMaTrangThai());
    }

    @Test
    @SuppressWarnings("unchecked")
    void allFiltersAndDeliveryTypeUseOneBasedPaginationWithStableSort() {
        when(invoices.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
        service.timKiem(null, null, null, "all", "all", 0, 1000);
        service.timKiem(null, null, null, "pending", "delivery", 2, 10);
        var captured = ArgumentCaptor.forClass(Pageable.class);
        verify(invoices, times(2)).findAll(any(Specification.class), captured.capture());
        assertEquals(0, captured.getAllValues().get(0).getPageNumber());
        assertEquals(100, captured.getAllValues().get(0).getPageSize());
        assertEquals(1, captured.getAllValues().get(1).getPageNumber());
        assertNotNull(captured.getValue().getSort().getOrderFor("id"));
        assertThrows(AppException.class, () -> service.timKiem(null, LocalDate.of(2026, 2, 2), LocalDate.of(2026, 1, 1), null, null, 1, 10));
        assertThrows(AppException.class, () -> service.timKiem(null, null, null, "unknown", null, 1, 10));
    }

    @Test
    void detailDerivesHistoricalDiscountAndIsNullSafeOnOriginalSchema() {
        HoaDon invoice = new HoaDon();
        invoice.setLoaiHoaDon(2);
        invoice.setTongTien(new BigDecimal("100000"));
        invoice.setPhiVanChuyen(new BigDecimal("20000"));
        invoice.setThanhTien(new BigDecimal("90000"));
        var detail = HoaDonDetailDto.from(invoice, null, null, null);
        assertEquals(new BigDecimal("30000"), detail.getTienGiamGia());
        assertEquals("Giao hàng", detail.getLoaiHoaDon());
        assertTrue(detail.getChiTietHoaDon().isEmpty());
        assertTrue(detail.getLichSuThanhToan().isEmpty());
        assertTrue(detail.getLichSuHoaDon().isEmpty());
        assertEquals(new BigDecimal("30000"), HoaDonListDto.from(invoice).getTienGiamGia());
        invoice.setTienGiamGia(new BigDecimal("25000"));
        assertEquals(new BigDecimal("25000"), HoaDonDetailDto.from(invoice, List.of()).getTienGiamGia());
    }

    @Test
    void paymentMethodFallsBackToPersistedParentInvoice() {
        HoaDon invoice = new HoaDon();
        PhuongThucThanhToan method = new PhuongThucThanhToan();
        method.setTenPhuongThuc("Tiền mặt");
        invoice.setIdPhuongThucThanhToan(method);
        LichSuThanhToan payment = new LichSuThanhToan();
        payment.setIdHoaDon(invoice);
        payment.setTrangThai(1);
        assertEquals("Tiền mặt", LichSuThanhToanDto.from(payment).getPhuongThucThanhToan());
        assertEquals("Đã thanh toán", LichSuThanhToanDto.from(payment).getTrangThai());
        payment.setIdHoaDon(null);
        assertEquals("Chưa cập nhật", LichSuThanhToanDto.from(payment).getPhuongThucThanhToan());
    }
}
