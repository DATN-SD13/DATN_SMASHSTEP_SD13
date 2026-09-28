package com.smashstep.datn.product.repository;

import com.smashstep.datn.product.entity.HinhAnhSanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HinhAnhSanPhamRepository extends JpaRepository<HinhAnhSanPham, Long> {
}
