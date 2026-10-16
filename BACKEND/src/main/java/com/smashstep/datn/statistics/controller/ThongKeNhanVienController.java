
package com.smashstep.datn.statistics.controller;

import com.smashstep.datn.common.response.ApiResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/thong-ke-tong-hop")
public class ThongKeNhanVienController {

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/nhan-vien")
    @Transactional(readOnly = true)
    public ApiResponse<List<Map<String, Object>>> thongKeNhanVien(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate tuNgay,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate denNgay
    ) {

        if (tuNgay.isAfter(denNgay)) {
            throw com.smashstep.datn.common.exception.AppException.badRequest("Từ ngày không được lớn hơn đến ngày");
        }

        List<?> rows = entityManager.createNativeQuery("""
                SELECT
                    nv.id,
                    nv.ma_nhan_vien,
                    nv.ten_nhan_vien,
                    COUNT_BIG(hd.id) AS tong_don,
                    COALESCE(SUM(hd.thanh_tien), 0) AS doanh_thu,
                    COALESCE(SUM(ct.so_luong_ban), 0) AS san_pham_ban
                FROM hoa_don hd

                JOIN nhan_vien nv
                    ON nv.id = hd.id_nhan_vien

                LEFT JOIN (
                    SELECT
                        id_hoa_don,
                        SUM(CAST(so_luong AS BIGINT)) AS so_luong_ban
                    FROM hoa_don_chi_tiet
                    GROUP BY id_hoa_don
                ) ct ON ct.id_hoa_don = hd.id

                WHERE hd.trang_thai = 5
                  AND hd.ngay_thanh_toan >= :tuNgay
                  AND hd.ngay_thanh_toan < :denNgay

                GROUP BY
                    nv.id,
                    nv.ma_nhan_vien,
                    nv.ten_nhan_vien

                ORDER BY doanh_thu DESC
                """)
                .setParameter("tuNgay", tuNgay.atStartOfDay())
                .setParameter(
                        "denNgay",
                        denNgay.plusDays(1).atStartOfDay()
                )
                .getResultList();

        List<Map<String, Object>> result = new ArrayList<>();

        for (Object row : rows) {
            Object[] values = (Object[]) row;

            Map<String, Object> employee = new LinkedHashMap<>();

            employee.put("id", values[0]);
            employee.put("code", values[1]);
            employee.put("name", values[2]);
            employee.put("totalOrders", values[3]);
            employee.put("revenue", values[4]);
            employee.put("productsSold", values[5]);

            result.add(employee);
        }

        return ApiResponse.ok(
                "Lấy thống kê doanh thu nhân viên thành công",
                result
        );
    }
}
