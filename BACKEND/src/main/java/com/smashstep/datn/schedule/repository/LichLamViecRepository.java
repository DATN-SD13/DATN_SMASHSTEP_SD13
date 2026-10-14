package com.smashstep.datn.schedule.repository;

import com.smashstep.datn.schedule.entity.LichLamViec;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LichLamViecRepository extends JpaRepository<LichLamViec, Long> {

    @Query("""
        SELECT l FROM LichLamViec l
        JOIN FETCH l.idNhanVien nv
        JOIN FETCH l.idCaLamViec ca
        WHERE (:tuNgay IS NULL OR l.ngayLam >= :tuNgay)
          AND (:denNgay IS NULL OR l.ngayLam <= :denNgay)
          AND (:caId IS NULL OR ca.id = :caId)
          AND (:tuKhoa IS NULL OR :tuKhoa = ''
               OR LOWER(nv.maNhanVien) LIKE LOWER(CONCAT('%', :tuKhoa, '%'))
               OR LOWER(nv.tenNhanVien) LIKE LOWER(CONCAT('%', :tuKhoa, '%')))
        ORDER BY l.ngayLam ASC, ca.gioBatDau ASC
        """)
    Page<LichLamViec> search(
            @Param("tuNgay")  LocalDate tuNgay,
            @Param("denNgay") LocalDate denNgay,
            @Param("caId")    Long caId,
            @Param("tuKhoa")  String tuKhoa,
            Pageable pageable
    );

    @Query("""
        SELECT l FROM LichLamViec l
        JOIN FETCH l.idNhanVien nv
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
}
