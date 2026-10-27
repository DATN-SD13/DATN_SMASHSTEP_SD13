package com.smashstep.datn.schedule.service;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Cash flows, not order-status revenue. Successful receipts and refunds are counted once. */
@Service
public class TienCaService {
    @PersistenceContext private EntityManager entityManager;
    public record Tien(BigDecimal mat,BigDecimal nganHang,BigDecimal khac) {
        public BigDecimal tong(){return mat.add(nganHang).add(khac);}
    }
    public Tien tinh(Long employee,LocalDateTime from,LocalDateTime to) {
        if(!to.isAfter(from))return new Tien(BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO);
        Object[] values=(Object[])entityManager.createNativeQuery("""
            WITH receipts AS (
              SELECT p.ma_phuong_thuc AS method,
                     CASE WHEN ls.trang_thai=2 THEN -ABS(COALESCE(ls.so_tien,0))
                          ELSE COALESCE(ls.so_tien,0) END AS amount
              FROM lich_su_thanh_toan ls JOIN hoa_don h ON h.id=ls.id_hoa_don
              LEFT JOIN phuong_thuc_thanh_toan p ON p.id=ls.id_phuong_thuc_thanh_toan
              WHERE h.id_nhan_vien=:employee AND ls.trang_thai IN (1,2)
                AND ls.thoi_gian>=:fromTime AND ls.thoi_gian<:toTime
              UNION ALL
              SELECT p.ma_phuong_thuc,COALESCE(h.thanh_tien,0)
              FROM hoa_don h LEFT JOIN phuong_thuc_thanh_toan p ON p.id=h.id_phuong_thuc_thanh_toan
              WHERE h.id_nhan_vien=:employee AND h.trang_thai IN (1,2,3,4,5,7)
                AND h.ngay_thanh_toan>=:fromTime AND h.ngay_thanh_toan<:toTime
                AND NOT EXISTS (SELECT 1 FROM lich_su_thanh_toan ls WHERE ls.id_hoa_don=h.id AND ls.trang_thai=1)
            )
            SELECT COALESCE(SUM(CASE WHEN method='TIEN_MAT' THEN amount ELSE 0 END),0),
                   COALESCE(SUM(CASE WHEN method='CHUYEN_KHOAN' THEN amount ELSE 0 END),0),
                   COALESCE(SUM(CASE WHEN method IS NULL OR method NOT IN ('TIEN_MAT','CHUYEN_KHOAN') THEN amount ELSE 0 END),0)
            FROM receipts
            """).setParameter("employee",employee).setParameter("fromTime",from).setParameter("toTime",to)
                .setHint("jakarta.persistence.query.timeout",15000).getSingleResult();
        return new Tien(amount(values[0]),amount(values[1]),amount(values[2]));
    }
    private static BigDecimal amount(Object value){return value==null?BigDecimal.ZERO:new BigDecimal(value.toString());}
}
