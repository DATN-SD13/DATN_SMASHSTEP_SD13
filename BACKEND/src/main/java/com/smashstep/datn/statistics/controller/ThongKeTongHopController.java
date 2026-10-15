
package com.smashstep.datn.statistics.controller;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.common.response.ApiResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/thong-ke-tong-hop")
public class ThongKeTongHopController {

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/bieu-do")
    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> getCharts(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate tuNgay,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate denNgay,

            @RequestParam(defaultValue = "day")
            String nhomTheo,

            @RequestParam(required = false)
            Long thuongHieuId
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

        // Chỉ chấp nhận 3 kiểu nhóm ngày/tháng/năm
        String groupExpression = switch (nhomTheo) {
            case "month" ->
                    "CONVERT(VARCHAR(7), hd.ngay_thanh_toan, 120)";
            case "year" ->
                    "CONVERT(VARCHAR(4), YEAR(hd.ngay_thanh_toan))";
            case "day" ->
                    "CONVERT(VARCHAR(10), hd.ngay_thanh_toan, 23)";
            default ->
                    throw AppException.badRequest(
                            "Kiểu thống kê không hợp lệ"
                    );
        };

        LocalDateTime fromTime = start.atStartOfDay();
        LocalDateTime toTime = end.plusDays(1).atStartOfDay();

        // Chỉ tính hóa đơn hoàn thành đã thanh toán
        String fromSql = """
            FROM hoa_don_chi_tiet ct
            JOIN hoa_don hd ON hd.id = ct.id_hoa_don
            JOIN san_pham_chi_tiet spct
                ON spct.id = ct.id_san_pham_chi_tiet
            JOIN san_pham sp
                ON sp.id = spct.id_san_pham
            LEFT JOIN thuong_hieu th
                ON th.id = sp.id_thuong_hieu
            WHERE hd.trang_thai = 5
              AND hd.ngay_thanh_toan IS NOT NULL
              AND hd.ngay_thanh_toan >= :fromTime
              AND hd.ngay_thanh_toan < :toTime
            """;

        if (thuongHieuId != null) {
            fromSql += " AND th.id = :brandId ";
        }

        // Biểu đồ sản phẩm bán được theo thời gian
        String salesSql = """
            SELECT %s AS nhan,
                   SUM(CAST(ct.so_luong AS BIGINT)) AS so_luong
            %s
            GROUP BY %s
            ORDER BY nhan
            """.formatted(
                groupExpression,
                fromSql,
                groupExpression
        );

        Query salesQuery =
                entityManager.createNativeQuery(salesSql);

        setParameters(
                salesQuery,
                fromTime,
                toTime,
                thuongHieuId
        );

        List<Map<String, Object>> sales = new ArrayList<>();

        for (Object item : salesQuery.getResultList()) {
            Object[] row = (Object[]) item;

            Map<String, Object> value = new LinkedHashMap<>();
            value.put("label", row[0]);
            value.put("quantity", row[1]);

            sales.add(value);
        }

        // Biểu đồ doanh thu và số lượng theo thương hiệu
        String brandSql = """
            SELECT COALESCE(th.ten_thuong_hieu, N'Khác'),
                   SUM(CAST(ct.so_luong AS BIGINT)),
                   SUM(COALESCE(ct.thanh_tien, 0))
            %s
            GROUP BY th.id, th.ten_thuong_hieu
            ORDER BY 3 DESC
            """.formatted(fromSql);

        Query brandQuery =
                entityManager.createNativeQuery(brandSql);

        setParameters(
                brandQuery,
                fromTime,
                toTime,
                thuongHieuId
        );

        List<Map<String, Object>> brands = new ArrayList<>();

        for (Object item : brandQuery.getResultList()) {
            Object[] row = (Object[]) item;

            Map<String, Object> value = new LinkedHashMap<>();
            value.put("name", row[0]);
            value.put("quantity", row[1]);
            value.put("revenue", row[2]);

            brands.add(value);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sales", sales);
        result.put("brands", brands);

        return ApiResponse.ok(
                "Lấy dữ liệu biểu đồ thành công",
                result
        );
    }

    private void setParameters(
            Query query,
            LocalDateTime fromTime,
            LocalDateTime toTime,
            Long brandId
    ) {
        query.setParameter("fromTime", fromTime);
        query.setParameter("toTime", toTime);

        if (brandId != null) {
            query.setParameter("brandId", brandId);
        }
    }
}
