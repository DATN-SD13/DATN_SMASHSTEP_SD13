package com.smashstep.datn.promotion.repository;

import com.smashstep.datn.promotion.entity.DotGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

@Repository
public interface DotGiamGiaRepository
        extends JpaRepository<DotGiamGia, Long>,
        JpaSpecificationExecutor<DotGiamGia> {

    boolean existsByMaDotGiamGia(String maDotGiamGia);
    Optional<DotGiamGia> findTopByOrderByIdDesc();
    Optional<DotGiamGia> findByMaDotGiamGia(String maDotGiamGia);

    @Query("select d.maDotGiamGia from DotGiamGia d")
    List<String> findAllCodes();
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
    org.springframework.data.domain.Page<DotGiamGia> search(
            @org.springframework.data.repository.query.Param("ma") String ma,
            @org.springframework.data.repository.query.Param("ten") String ten,
            @org.springframework.data.repository.query.Param("trangThai") Integer trangThai,
            @org.springframework.data.repository.query.Param("tuNgay") java.time.LocalDateTime tuNgay,
            @org.springframework.data.repository.query.Param("denNgay") java.time.LocalDateTime denNgay,
            org.springframework.data.domain.Pageable pageable
    );
}
