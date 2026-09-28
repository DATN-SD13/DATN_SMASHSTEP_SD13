package com.smashstep.datn.invoice.repository;

import com.smashstep.datn.invoice.entity.LichSuHoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LichSuHoaDonRepository extends JpaRepository<LichSuHoaDon, Long> {
}
