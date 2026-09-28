package com.smashstep.datn.payment.repository;

import com.smashstep.datn.payment.entity.HinhThucThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HinhThucThanhToanRepository extends JpaRepository<HinhThucThanhToan, Long> {
}
