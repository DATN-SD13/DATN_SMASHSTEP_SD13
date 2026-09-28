package com.smashstep.datn.product.repository;

import com.smashstep.datn.product.entity.SanPhamChiTiet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SanPhamChiTietRepository extends JpaRepository<SanPhamChiTiet, Long> {
}
