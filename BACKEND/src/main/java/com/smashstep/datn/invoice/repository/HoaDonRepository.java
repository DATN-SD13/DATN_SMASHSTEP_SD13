package com.smashstep.datn.invoice.repository;

import com.smashstep.datn.invoice.entity.HoaDon;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HoaDonRepository extends JpaRepository<HoaDon, Long>, JpaSpecificationExecutor<HoaDon> {
    @Override
    @EntityGraph(attributePaths = {"idKhachHang", "idNhanVien", "idPhuongThucThanhToan"})
    Page<HoaDon> findAll(Specification<HoaDon> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"idKhachHang", "idNhanVien", "idPhuongThucThanhToan"})
    Optional<HoaDon> findByMaHoaDon(String maHoaDon);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"idKhachHang", "idNhanVien", "idPhuongThucThanhToan"})
    @Query("select h from HoaDon h where h.maHoaDon = :maHoaDon")
    Optional<HoaDon> findByMaHoaDonForUpdate(@Param("maHoaDon") String maHoaDon);

    /** Tổng số đơn hoàn thành (trangThai = 5) của khách hàng */
    @Query("SELECT COUNT(h) FROM HoaDon h WHERE h.idKhachHang.id = :khachHangId AND h.trangThai = 5")
    long countDonHoanThanhByKhachHang(@Param("khachHangId") Long khachHangId);

    /** Ngày thanh toán gần nhất của khách hàng (đơn hoàn thành) */
    @Query("SELECT MAX(h.ngayThanhToan) FROM HoaDon h WHERE h.idKhachHang.id = :khachHangId AND h.trangThai = 5")
    java.time.LocalDateTime findNgayMuaGanNhatByKhachHang(@Param("khachHangId") Long khachHangId);
}
