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
    public Tien tinh(Long employee,Long shift,LocalDateTime from,LocalDateTime to) {
        if(!to.isAfter(from))return new Tien(BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO);
        @SuppressWarnings("unchecked")
        java.util.List<Object[]> values=entityManager.createNativeQuery("""
            WITH receipts AS (
              SELECT h.id_nhan_vien AS owner, audit.nguoi_tao AS actor,
                     audit.ghi_chu AS allocation, p.ma_phuong_thuc AS method,
                     ls.trang_thai AS status, COALESCE(ls.so_tien,0) AS amount, ls.mo_ta AS description
              FROM lich_su_thanh_toan ls JOIN hoa_don h ON h.id=ls.id_hoa_don
              LEFT JOIN phuong_thuc_thanh_toan p ON p.id=ls.id_phuong_thuc_thanh_toan
              LEFT JOIN lich_su_hoa_don audit ON audit.id_hoa_don=h.id
                AND LEFT(audit.ghi_chu,LEN(CONCAT('[SHIFT-PAYMENT:1]|',ls.id,'|')))=CONCAT('[SHIFT-PAYMENT:1]|',ls.id,'|')
              WHERE ls.trang_thai IN (1,2)
                AND ls.thoi_gian>=:fromTime AND ls.thoi_gian<:toTime
              UNION ALL
              SELECT h.id_nhan_vien,NULL,NULL,p.ma_phuong_thuc,1,COALESCE(h.thanh_tien,0),NULL
              FROM hoa_don h LEFT JOIN phuong_thuc_thanh_toan p ON p.id=h.id_phuong_thuc_thanh_toan
              WHERE h.id_nhan_vien=:employee AND h.trang_thai IN (1,2,3,4,5,7)
                AND h.ngay_thanh_toan>=:fromTime AND h.ngay_thanh_toan<:toTime
                AND NOT EXISTS (SELECT 1 FROM lich_su_thanh_toan ls WHERE ls.id_hoa_don=h.id AND ls.trang_thai=1)
            )
            SELECT owner,actor,allocation,method,status,amount,description FROM receipts
            """).setParameter("employee",employee).setParameter("fromTime",from).setParameter("toTime",to)
                .setHint("jakarta.persistence.query.timeout",15000).getResultList();
        return TienCaRules.total(employee,shift,values.stream().map(row -> new TienCaRules.Payment(
                id(row[0]),id(row[1]),(String)row[2],(String)row[3],((Number)row[4]).intValue(),amount(row[5]),(String)row[6])).toList());
    }
    private static BigDecimal amount(Object value){return value==null?BigDecimal.ZERO:new BigDecimal(value.toString());}
    private static Long id(Object value){return value==null?null:((Number)value).longValue();}
}
