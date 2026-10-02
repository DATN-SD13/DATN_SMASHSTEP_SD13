package com.smashstep.datn.invoice.repository;

import com.smashstep.datn.invoice.entity.HoaDon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HoaDonRepository extends JpaRepository<HoaDon, Long>, JpaSpecificationExecutor<HoaDon> {
    @Override
    @EntityGraph(attributePaths = {"idKhachHang", "idNhanVien", "idPhuongThucThanhToan"})
    Page<HoaDon> findAll(Specification<HoaDon> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"idKhachHang", "idNhanVien", "idPhuongThucThanhToan"})
    Optional<HoaDon> findByMaHoaDon(String maHoaDon);
}
