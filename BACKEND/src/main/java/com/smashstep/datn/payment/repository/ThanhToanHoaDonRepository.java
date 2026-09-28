package com.smashstep.datn.payment.repository;

import com.smashstep.datn.payment.entity.ThanhToanHoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ThanhToanHoaDonRepository extends JpaRepository<ThanhToanHoaDon, Long> {
}
