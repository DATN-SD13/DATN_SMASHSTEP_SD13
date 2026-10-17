
package com.smashstep.datn.statistics.controller;

import com.smashstep.datn.common.response.ApiResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/thong-ke-tong-hop")
public class ThongKeSanPhamController {

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/san-pham")
    @Transactional(readOnly = true)
    public ApiResponse<List<Map<String, Object>>> thongKeSanPham(
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
                sp.id,
                sp.ma_san_pham,
                sp.ten_san_pham,

                COALESCE(ban.so_luong_ban, 0),
                COALESCE(ban.doanh_thu, 0),
                COALESCE(kho.ton_kho, 0),
                COALESCE(hoan.so_luong_hoan_tien, 0)

            FROM san_pham sp

            LEFT JOIN (
                SELECT
                    ctsp.id_san_pham,
                    SUM(CAST(cthd.so_luong AS BIGINT))
                        AS so_luong_ban,
                    SUM(COALESCE(cthd.thanh_tien, 0))
                        AS doanh_thu

                FROM hoa_don_chi_tiet cthd

                JOIN hoa_don hd
                    ON hd.id = cthd.id_hoa_don

                JOIN san_pham_chi_tiet ctsp
                    ON ctsp.id = cthd.id_san_pham_chi_tiet

                WHERE hd.trang_thai = 5
                  AND hd.ngay_thanh_toan >= :tuNgay
                  AND hd.ngay_thanh_toan < :denNgay

                GROUP BY ctsp.id_san_pham
            ) ban
                ON ban.id_san_pham = sp.id

            LEFT JOIN (
                SELECT
                    id_san_pham,
                    SUM(CAST(COALESCE(so_luong, 0) AS BIGINT))
                        AS ton_kho

                FROM san_pham_chi_tiet
                GROUP BY id_san_pham
            ) kho
                ON kho.id_san_pham = sp.id

            LEFT JOIN (
                SELECT
                    ctsp.id_san_pham,
                    SUM(CAST(cthd.so_luong AS BIGINT))
                        AS so_luong_hoan_tien

                FROM hoa_don_chi_tiet cthd

                JOIN hoa_don hd
                    ON hd.id = cthd.id_hoa_don

                JOIN san_pham_chi_tiet ctsp
                    ON ctsp.id = cthd.id_san_pham_chi_tiet

                WHERE hd.trang_thai = 7
                  AND hd.ngay_cap_nhat >= :tuNgay
                  AND hd.ngay_cap_nhat < :denNgay

                GROUP BY ctsp.id_san_pham
            ) hoan
                ON hoan.id_san_pham = sp.id

            ORDER BY
                COALESCE(ban.so_luong_ban, 0) DESC,
                sp.ten_san_pham ASC
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

            Map<String, Object> product = new LinkedHashMap<>();

            product.put("id", values[0]);
            product.put("code", values[1]);
            product.put("name", values[2]);
            product.put("sold", values[3]);
            product.put("revenue", values[4]);
            product.put("stock", values[5]);
            product.put("returned", values[6]);

            result.add(product);
        }

        return ApiResponse.ok(
                "Lấy thống kê sản phẩm thành công",
                result
        );
    }
}
