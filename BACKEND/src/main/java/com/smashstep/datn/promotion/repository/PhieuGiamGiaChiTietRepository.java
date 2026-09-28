package com.smashstep.datn.promotion.repository;

import com.smashstep.datn.promotion.entity.PhieuGiamGiaChiTiet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhieuGiamGiaChiTietRepository extends JpaRepository<PhieuGiamGiaChiTiet, Long> {
}
