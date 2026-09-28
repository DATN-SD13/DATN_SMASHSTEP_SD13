package com.smashstep.datn.invoice.repository;

import com.smashstep.datn.invoice.entity.ChiTietHoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChiTietHoaDonRepository extends JpaRepository<ChiTietHoaDon, Long> {
}
