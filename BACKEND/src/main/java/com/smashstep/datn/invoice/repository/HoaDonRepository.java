package com.smashstep.datn.invoice.repository;

import com.smashstep.datn.invoice.entity.HoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HoaDonRepository extends JpaRepository<HoaDon, Long> {
}
