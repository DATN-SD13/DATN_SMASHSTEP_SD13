
package com.smashstep.datn.statistics.controller;

import com.smashstep.datn.common.response.ApiResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.smashstep.datn.common.exception.AppException;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/thong-ke-tong-hop")
public class ThongKeChuKyController {

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/chu-ky")
    @Transactional(readOnly = true)
    public ApiResponse<List<Map<String, Object>>> getChuKy() {

        LocalDate today = LocalDate.now();

        LocalDate weekStart = today.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        );

        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate yearStart = today.withDayOfYear(1);

        List<Map<String, Object>> result = new ArrayList<>();

        result.add(createPeriod(
                "Hôm nay",
                today,
                today.plusDays(1),
                today.minusDays(1),
                today
        ));

        result.add(createPeriod(
                "Tuần này",
                weekStart,
                weekStart.plusWeeks(1),
                weekStart.minusWeeks(1),
                weekStart
        ));

        result.add(createPeriod(
                "Tháng này",
                monthStart,
                monthStart.plusMonths(1),
                monthStart.minusMonths(1),
                monthStart
        ));

        result.add(createPeriod(
                "Năm nay",
                yearStart,
                yearStart.plusYears(1),
                yearStart.minusYears(1),
                yearStart
        ));

        return ApiResponse.ok(
                "Lấy thống kê theo chu kỳ thành công",
                result
        );
    }

    @GetMapping("/so-sanh-doanh-thu")
    @Transactional(readOnly = true)
    public ApiResponse<List<Map<String, Object>>> soSanhDoanhThu(
            @RequestParam(defaultValue = "month") String nhomTheo,
            @RequestParam(required = false) Integer nam
    ) {
        int year = nam != null ? nam : LocalDate.now().getYear();

        if (year < 2000 || year > 2100) {
            throw AppException.badRequest("Năm thống kê không hợp lệ");
        }

        List<Map<String, Object>> result = new ArrayList<>();

        switch (nhomTheo) {
            case "month" -> {
                // So sánh 12 tháng trong năm
                for (int month = 1; month <= 12; month++) {
                    LocalDate start = LocalDate.of(year, month, 1);
                    LocalDate end = start.plusMonths(1);

                    result.add(createPeriod(
                            "Tháng " + month,
                            start,
                            end,
                            start.minusMonths(1),
                            start
                    ));
                }
            }

            case "quarter" -> {
                // So sánh 4 quý trong năm
                for (int quarter = 1; quarter <= 4; quarter++) {
                    int firstMonth = (quarter - 1) * 3 + 1;

                    LocalDate start = LocalDate.of(year, firstMonth, 1);
                    LocalDate end = start.plusMonths(3);

                    result.add(createPeriod(
                            "Quý " + quarter,
                            start,
                            end,
                            start.minusMonths(3),
                            start
                    ));
                }
            }

            case "year" -> {
                // So sánh 5 năm gần nhất tính đến năm được chọn
                for (int y = year - 4; y <= year; y++) {
                    LocalDate start = LocalDate.of(y, 1, 1);
                    LocalDate end = start.plusYears(1);

                    result.add(createPeriod(
                            "Năm " + y,
                            start,
                            end,
                            start.minusYears(1),
                            start
                    ));
                }
            }

            default -> throw AppException.badRequest(
                    "Chỉ hỗ trợ thống kê theo tháng, quý hoặc năm"
            );
        }

        return ApiResponse.ok(
                "Lấy dữ liệu so sánh doanh thu thành công",
                result
        );
    }

    private Map<String, Object> createPeriod(
            String name,
            LocalDate start,
            LocalDate end,
            LocalDate previousStart,
            LocalDate previousEnd
    ) {
        Object[] current = queryRevenue(start, end);
        Object[] previous = queryRevenue(
                previousStart,
                previousEnd
        );

        long totalOrders = ((Number) current[0]).longValue();

        BigDecimal revenue = new BigDecimal(
                current[1].toString()
        );

        BigDecimal previousRevenue = new BigDecimal(
                previous[1].toString()
        );

        BigDecimal aov = totalOrders == 0
                ? BigDecimal.ZERO
                : revenue.divide(
                BigDecimal.valueOf(totalOrders),
                0,
                RoundingMode.HALF_UP
        );

        BigDecimal growth = null;

        if (previousRevenue.compareTo(BigDecimal.ZERO) > 0) {
            growth = revenue
                    .subtract(previousRevenue)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(
                            previousRevenue,
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        Map<String, Object> row = new LinkedHashMap<>();

        row.put("period", name);
        row.put("revenue", revenue);
        row.put("orders", totalOrders);
        row.put("aov", aov);
        row.put("growth", growth);

        return row;
    }

    private Object[] queryRevenue(
            LocalDate start,
            LocalDate end
    ) {
        return (Object[]) entityManager
                .createNativeQuery("""
                SELECT
                    COUNT_BIG(hd.id),
                    COALESCE(SUM(hd.thanh_tien), 0)
                FROM hoa_don hd
                WHERE hd.trang_thai = 5
                  AND hd.ngay_thanh_toan IS NOT NULL
                  AND hd.ngay_thanh_toan >= :startDate
                  AND hd.ngay_thanh_toan < :endDate
            """)
                .setParameter(
                        "startDate",
                        start.atStartOfDay()
                )
                .setParameter(
                        "endDate",
                        end.atStartOfDay()
                )
                .getSingleResult();
    }
}
