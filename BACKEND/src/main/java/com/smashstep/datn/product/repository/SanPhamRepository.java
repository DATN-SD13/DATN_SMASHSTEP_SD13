package com.smashstep.datn.product.repository;

import com.smashstep.datn.product.entity.SanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SanPhamRepository extends JpaRepository<SanPham, Long> {
}
