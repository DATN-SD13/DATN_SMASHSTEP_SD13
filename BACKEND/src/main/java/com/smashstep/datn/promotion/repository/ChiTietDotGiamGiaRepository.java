package com.smashstep.datn.promotion.repository;

import com.smashstep.datn.promotion.entity.ChiTietDotGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChiTietDotGiamGiaRepository extends JpaRepository<ChiTietDotGiamGia, Long> {
}
