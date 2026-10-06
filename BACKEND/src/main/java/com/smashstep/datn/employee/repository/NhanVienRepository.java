package com.smashstep.datn.employee.repository;

import com.smashstep.datn.employee.entity.NhanVien;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NhanVienRepository extends JpaRepository<NhanVien, Long> {

    @Query("""
            SELECT nv
            FROM NhanVien nv
            LEFT JOIN nv.idVaiTro vt
            WHERE
                (
                    :tuKhoa IS NULL
                    OR :tuKhoa = ''
                    OR LOWER(COALESCE(nv.maNhanVien, ''))
                        LIKE LOWER(CONCAT('%', :tuKhoa, '%'))
                    OR LOWER(COALESCE(nv.tenNhanVien, ''))
                        LIKE LOWER(CONCAT('%', :tuKhoa, '%'))
                    OR LOWER(COALESCE(nv.tenDangNhap, ''))
                        LIKE LOWER(CONCAT('%', :tuKhoa, '%'))
                    OR LOWER(COALESCE(nv.email, ''))
                        LIKE LOWER(CONCAT('%', :tuKhoa, '%'))
                    OR LOWER(COALESCE(nv.soDienThoai, ''))
                        LIKE LOWER(CONCAT('%', :tuKhoa, '%'))
                )
                AND (:vaiTroId IS NULL OR vt.id = :vaiTroId)
                AND (:trangThai IS NULL OR nv.trangThai = :trangThai)
            """)
    Page<NhanVien> timKiem(
            @Param("tuKhoa") String tuKhoa,
            @Param("vaiTroId") Long vaiTroId,
            @Param("trangThai") Integer trangThai,
            Pageable pageable
    );

    Optional<NhanVien> findByMaNhanVien(String maNhanVien);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByMaNhanVien(String maNhanVien);

    boolean existsByTenDangNhap(String tenDangNhap);
}