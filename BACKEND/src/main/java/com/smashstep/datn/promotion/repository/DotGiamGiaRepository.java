package com.smashstep.datn.promotion.repository;

import com.smashstep.datn.promotion.entity.DotGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DotGiamGiaRepository extends JpaRepository<DotGiamGia, Long> {
}
