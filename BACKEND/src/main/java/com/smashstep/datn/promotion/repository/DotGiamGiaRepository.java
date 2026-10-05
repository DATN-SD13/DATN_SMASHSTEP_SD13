package com.smashstep.datn.promotion.repository;

import com.smashstep.datn.promotion.entity.DotGiamGia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface DotGiamGiaRepository extends JpaRepository<DotGiamGia, Long> {

    @Query("""
            SELECT d
            FROM DotGiamGia d
            WHERE (:ma IS NULL OR :ma = '' OR LOWER(d.maDotGiamGia) LIKE LOWER(CONCAT('%', :ma, '%')))
              AND (:ten IS NULL OR :ten = '' OR LOWER(d.tenDotGiamGia) LIKE LOWER(CONCAT('%', :ten, '%')))
              AND (:trangThai IS NULL OR d.trangThai = :trangThai)
              AND (:tuNgay IS NULL OR d.ngayBatDau >= :tuNgay)
              AND (:denNgay IS NULL OR d.ngayKetThuc <= :denNgay)
            ORDER BY d.ngayTao DESC
            """)
    Page<DotGiamGia> search(
            @Param("ma") String ma,
            @Param("ten") String ten,
            @Param("trangThai") Integer trangThai,
            @Param("tuNgay") LocalDateTime tuNgay,
            @Param("denNgay") LocalDateTime denNgay,
            Pageable pageable
    );
}