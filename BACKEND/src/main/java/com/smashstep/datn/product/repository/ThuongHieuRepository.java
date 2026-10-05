package com.smashstep.datn.product.repository;

import com.smashstep.datn.product.entity.ThuongHieu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ThuongHieuRepository extends JpaRepository<ThuongHieu, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<ThuongHieu> {
}
