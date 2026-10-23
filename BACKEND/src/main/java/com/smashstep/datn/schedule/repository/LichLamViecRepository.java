package com.smashstep.datn.schedule.repository;

import com.smashstep.datn.schedule.entity.LichLamViec;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LichLamViecRepository extends JpaRepository<LichLamViec, Long> {

    @Query("""
        SELECT l FROM LichLamViec l
        JOIN FETCH l.idNhanVien nv
        LEFT JOIN FETCH nv.idVaiTro vt
        JOIN FETCH l.idCaLamViec ca
        WHERE (:tuNgay IS NULL OR l.ngayLam >= :tuNgay)
          AND (:denNgay IS NULL OR l.ngayLam <= :denNgay)
          AND (:caId IS NULL OR ca.id = :caId)
          AND (:vaiTroId IS NULL OR vt.id = :vaiTroId)
          AND (:tuKhoa IS NULL OR :tuKhoa = ''
               OR LOWER(nv.maNhanVien) LIKE LOWER(CONCAT('%', :tuKhoa, '%'))
               OR LOWER(nv.tenNhanVien) LIKE LOWER(CONCAT('%', :tuKhoa, '%')))
        ORDER BY l.ngayLam DESC, ca.gioBatDau ASC
        """)
    Page<LichLamViec> search(
            @Param("tuNgay")   LocalDate tuNgay,
            @Param("denNgay")  LocalDate denNgay,
            @Param("caId")     Long caId,
            @Param("vaiTroId") Long vaiTroId,
            @Param("tuKhoa")   String tuKhoa,
            Pageable pageable
    );

    @Query("""
        SELECT l FROM LichLamViec l
        JOIN FETCH l.idNhanVien nv
        LEFT JOIN FETCH nv.idVaiTro vt
        JOIN FETCH l.idCaLamViec ca
        WHERE l.ngayLam >= :tuNgay AND l.ngayLam <= :denNgay
        ORDER BY l.ngayLam ASC, ca.gioBatDau ASC
        """)
    List<LichLamViec> findByNgayLamBetween(
            @Param("tuNgay")  LocalDate tuNgay,
            @Param("denNgay") LocalDate denNgay
    );

    boolean existsByIdNhanVien_MaNhanVienAndIdCaLamViec_IdAndNgayLam(
            String maNhanVien, Long caId, LocalDate ngayLam);

    /**
     * Trả về danh sách mã nhân viên đã có lịch trong ca+ngày chỉ định.
     * Dùng để kiểm tra trùng ca khi thay thế nhân viên.
     */
    @Query("""
        SELECT nv.maNhanVien FROM LichLamViec l
        JOIN l.idNhanVien nv
        WHERE l.idCaLamViec.id = :caId
          AND l.ngayLam = :ngayLam
        """)
    List<String> findMaNhanVienTrongCa(
            @Param("caId")    Long caId,
            @Param("ngayLam") LocalDate ngayLam);

    /**
     * Lịch tương lai (ngayLam >= hôm nay) còn đang mở (trangThai 0 hoặc 1)
     * của nhân viên bị khóa (trangThai != 1). Dùng để hiển thị cảnh báo xếp lại.
     */
    @Query("""
        SELECT l FROM LichLamViec l
        JOIN FETCH l.idNhanVien nv
        LEFT JOIN FETCH nv.idVaiTro vt
        JOIN FETCH l.idCaLamViec ca
        WHERE nv.trangThai != 1
          AND l.trangThai IN (0, 1)
          AND l.ngayLam >= :tuNgay
        ORDER BY l.ngayLam ASC, ca.gioBatDau ASC
        """)
    List<LichLamViec> findLichCuaNhanVienBiKhoa(@Param("tuNgay") LocalDate tuNgay);

    /**
     * Lịch chưa giao ca (trangThai 0 hoặc 1) mà thời điểm kết thúc ca đã qua.
     * Dùng native query để tính datetime kết thúc chính xác (kể cả ca qua đêm).
     */
    @Query(value = """
        SELECT l.* FROM lich_lam_viec l
        JOIN ca_lam_viec c ON l.id_ca_lam_viec = c.id
        WHERE l.trang_thai IN (0, 1)
          AND (
            CASE
              WHEN c.gio_ket_thuc >= c.gio_bat_dau
                THEN CAST(CONVERT(varchar, l.ngay_lam, 23) + ' ' + CONVERT(varchar, c.gio_ket_thuc, 108) AS datetime2)
              ELSE
                CAST(CONVERT(varchar, DATEADD(day, 1, l.ngay_lam), 23) + ' ' + CONVERT(varchar, c.gio_ket_thuc, 108) AS datetime2)
            END
          ) <= :now
        """, nativeQuery = true)
    List<LichLamViec> findExpiredLich(@Param("now") LocalDateTime now);

    /**
     * Tính tổng doanh thu (thanhTien) của nhân viên trong khoảng thời gian ca.
     * Tính cả hóa đơn tại quầy đã thanh toán (trangThai=8, ngayThanhToan!=null)
     * và hóa đơn online hoàn thành (trangThai=5, ngayThanhToan!=null).
     */
    @Query("""
        SELECT COALESCE(SUM(h.thanhTien), 0)
        FROM HoaDon h
        WHERE h.idNhanVien.id = :nhanVienId
          AND h.ngayThanhToan >= :batDau
          AND h.ngayThanhToan < :ketThuc
          AND h.ngayThanhToan IS NOT NULL
          AND (h.trangThai = 5 OR h.trangThai = 8)
        """)
    BigDecimal tinhDoanhThu(
            @Param("nhanVienId") Long nhanVienId,
            @Param("batDau")     LocalDateTime batDau,
            @Param("ketThuc")    LocalDateTime ketThuc
    );
    @Query("select l from LichLamViec l join fetch l.idCaLamViec join fetch l.idNhanVien where l.idNhanVien.id=:employee and l.ngayLam between :from and :to order by l.ngayLam,l.idCaLamViec.gioBatDau")
    List<LichLamViec> findForEmployee(@Param("employee") Long employee,@Param("from") LocalDate from,@Param("to") LocalDate to);
    boolean existsByIdCaLamViec_Id(Long shift);

}
