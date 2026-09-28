package com.smashstep.datn.product.repository;

import com.smashstep.datn.product.entity.KieuDang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KieuDangRepository extends JpaRepository<KieuDang, Long> {
}
