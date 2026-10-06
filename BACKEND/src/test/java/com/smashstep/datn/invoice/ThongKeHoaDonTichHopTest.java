package com.smashstep.datn.invoice;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.invoice.service.ThongKeHoaDonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ThongKeHoaDonTichHopTest {
    @Autowired private ThongKeHoaDonService service;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void aggregatesRealInvoicesAndPaidRevenueWithoutChangingData() {
        var summary = service.tongHop(null, null, null, null);
        Long total = jdbc.queryForObject("select count_big(*) from hoa_don", Long.class);
        BigDecimal revenue = jdbc.queryForObject("select coalesce(sum(thanh_tien), 0) from hoa_don where trang_thai = 5 and ngay_thanh_toan is not null", BigDecimal.class);
        assertEquals(total.longValue(), summary.tongHoaDon());
        assertEquals(0, revenue.compareTo(summary.doanhThuDaThanhToan()));
        assertEquals(summary.tongHoaDon(), summary.theoTrangThai().stream().mapToLong(item -> item.soLuong()).sum());
        assertEquals(summary.tongHoaDon(), summary.theoLoaiDon().stream().mapToLong(item -> item.soLuong()).sum());
    }

    @Test
    void dateTypeAndCodeFiltersMatchRealDatabase() {
        var delivery = service.tongHop(null, null, null, "delivery");
        Long expected = jdbc.queryForObject("select count_big(*) from hoa_don where loai_hoa_don = 2", Long.class);
        assertEquals(expected.longValue(), delivery.tongHoaDon());
        assertEquals(0, service.tongHop(null, null, "__NO_INVOICE_STABILIZATION__", null).tongHoaDon());
        assertThrows(AppException.class, () -> service.tongHop(LocalDate.of(2026, 2, 2), LocalDate.of(2026, 1, 1), null, null));
    }
}
