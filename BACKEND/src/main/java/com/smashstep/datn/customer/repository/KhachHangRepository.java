package com.smashstep.datn.customer.repository;

import com.smashstep.datn.customer.entity.KhachHang;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KhachHangRepository extends JpaRepository<KhachHang, Long> {

    Optional<KhachHang> findByMaKhachHang(String maKhachHang);

    boolean existsByMaKhachHang(String maKhachHang);

    boolean existsByTenTaiKhoan(String tenTaiKhoan);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsBySoDienThoai(String soDienThoai);

    boolean existsBySoDienThoaiAndIdNot(String soDienThoai, Long id);

    @Query("""
        SELECT k FROM KhachHang k
        WHERE (
            :tuKhoa IS NULL OR :tuKhoa = '' OR
            LOWER(COALESCE(k.maKhachHang, '')) LIKE LOWER(CONCAT('%', :tuKhoa, '%')) OR
            LOWER(COALESCE(k.tenTaiKhoan, '')) LIKE LOWER(CONCAT('%', :tuKhoa, '%')) OR
            LOWER(COALESCE(k.tenKhachHang, '')) LIKE LOWER(CONCAT('%', :tuKhoa, '%')) OR
            LOWER(COALESCE(k.email, '')) LIKE LOWER(CONCAT('%', :tuKhoa, '%')) OR
            COALESCE(k.soDienThoai, '') LIKE CONCAT('%', :tuKhoa, '%')
        )
        AND (:trangThai IS NULL OR k.trangThai = :trangThai)
        """)
    Page<KhachHang> search(
            @Param("tuKhoa") String tuKhoa,
            @Param("trangThai") Integer trangThai,
            Pageable pageable
    );
}
