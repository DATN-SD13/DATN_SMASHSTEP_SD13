
package com.smashstep.datn.statistics.controller;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.common.response.ApiResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/thong-ke-tong-hop")
public class ThongKeTongQuanController {

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/tong-quan")
    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> tongQuan(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate tuNgay,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate denNgay
    ) {
        LocalDate start = tuNgay != null
                ? tuNgay
                : LocalDate.now().withDayOfMonth(1);

        LocalDate end = denNgay != null
                ? denNgay
                : LocalDate.now();

        if (start.isAfter(end)) {
            throw AppException.badRequest(
                    "Từ ngày không được lớn hơn đến ngày"
            );
        }

        var fromTime = start.atStartOfDay();
        var toTime = end.plusDays(1).atStartOfDay();

        // 1. Tổng doanh thu, tổng đơn, tiền mặt, chuyển khoản
        Object[] revenue = (Object[]) entityManager
                .createNativeQuery("""
                    SELECT
                        COUNT_BIG(hd.id),
                        COALESCE(SUM(hd.thanh_tien), 0),

                        COALESCE(SUM(
                            CASE
                                WHEN paid.id_hoa_don IS NOT NULL
                                    THEN paid.tien_mat
                                WHEN pt.ma_phuong_thuc = 'TIEN_MAT'
                                    THEN COALESCE(hd.thanh_tien, 0)
                                ELSE 0
                            END
                        ), 0),

                        COALESCE(SUM(
                            CASE
                                WHEN paid.id_hoa_don IS NOT NULL
                                    THEN paid.chuyen_khoan
                                WHEN pt.ma_phuong_thuc = 'CHUYEN_KHOAN'
                                    THEN COALESCE(hd.thanh_tien, 0)
                                ELSE 0
                            END
                        ), 0)

                    FROM hoa_don hd

                    LEFT JOIN phuong_thuc_thanh_toan pt
                        ON pt.id = hd.id_phuong_thuc_thanh_toan

                    LEFT JOIN (
                        SELECT
                            ls.id_hoa_don,
                            SUM(CASE
                                WHEN p.ma_phuong_thuc = 'TIEN_MAT'
                                THEN COALESCE(ls.so_tien, 0)
                                ELSE 0
                            END) AS tien_mat,

                            SUM(CASE
                                WHEN p.ma_phuong_thuc = 'CHUYEN_KHOAN'
                                THEN COALESCE(ls.so_tien, 0)
                                ELSE 0
                            END) AS chuyen_khoan

                        FROM lich_su_thanh_toan ls
                        LEFT JOIN phuong_thuc_thanh_toan p
                            ON p.id = ls.id_phuong_thuc_thanh_toan
                        WHERE ls.trang_thai = 1
                        GROUP BY ls.id_hoa_don
                    ) paid ON paid.id_hoa_don = hd.id

                    WHERE hd.trang_thai = 5
                      AND hd.ngay_thanh_toan IS NOT NULL
                      AND hd.ngay_thanh_toan >= :fromTime
                      AND hd.ngay_thanh_toan < :toTime
                    """)
                .setParameter("fromTime", fromTime)
                .setParameter("toTime", toTime)
                .getSingleResult();

        long totalOrders = ((Number) revenue[0]).longValue();
        BigDecimal totalRevenue = amount(revenue[1]);
        BigDecimal cashRevenue = amount(revenue[2]);
        BigDecimal transferRevenue = amount(revenue[3]);

        // 2. Tổng số sản phẩm bán được
        Number productsSold = (Number) entityManager
                .createNativeQuery("""
                    SELECT COALESCE(
                        SUM(CAST(ct.so_luong AS BIGINT)), 0
                    )
                    FROM hoa_don_chi_tiet ct
                    JOIN hoa_don hd
                        ON hd.id = ct.id_hoa_don
                    WHERE hd.trang_thai = 5
                      AND hd.ngay_thanh_toan IS NOT NULL
                      AND hd.ngay_thanh_toan >= :fromTime
                      AND hd.ngay_thanh_toan < :toTime
                    """)
                .setParameter("fromTime", fromTime)
                .setParameter("toTime", toTime)
                .getSingleResult();

        // 3. Khách hàng mới đăng ký trong khoảng thời gian
        Number newCustomers = (Number) entityManager
                .createNativeQuery("""
                    SELECT COUNT_BIG(*)
                    FROM khach_hang kh
                    WHERE kh.ngay_tao >= :fromTime
                      AND kh.ngay_tao < :toTime
                    """)
                .setParameter("fromTime", fromTime)
                .setParameter("toTime", toTime)
                .getSingleResult();

        // 4. Giá trị trung bình mỗi đơn
        BigDecimal averageOrderValue = totalOrders == 0
                ? BigDecimal.ZERO
                : totalRevenue.divide(
                BigDecimal.valueOf(totalOrders),
                0,
                RoundingMode.HALF_UP
        );

        // 5. Trả dữ liệu về frontend
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("totalRevenue", totalRevenue);
        result.put("cashRevenue", cashRevenue);
        result.put("transferRevenue", transferRevenue);
        result.put("totalOrders", totalOrders);
        result.put("averageOrderValue", averageOrderValue);
        result.put("productsSold", productsSold.longValue());
        result.put("newCustomers", newCustomers.longValue());

        return ApiResponse.ok(
                "Lấy tổng quan thống kê thành công",
                result
        );
    }

    private BigDecimal amount(Object value) {
        return value == null
                ? BigDecimal.ZERO
                : new BigDecimal(value.toString());
    }
}
