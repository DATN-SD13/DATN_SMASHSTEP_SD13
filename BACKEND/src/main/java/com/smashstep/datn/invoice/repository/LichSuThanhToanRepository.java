package com.smashstep.datn.invoice.repository;

import com.smashstep.datn.invoice.entity.LichSuThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LichSuThanhToanRepository extends JpaRepository<LichSuThanhToan, Long> {
    List<LichSuThanhToan> findByIdHoaDonIdOrderByThoiGianDesc(Long idHoaDon);
}
