package com.smashstep.datn.product.repository;

import com.smashstep.datn.product.entity.DanhMuc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DanhMucRepository extends JpaRepository<DanhMuc, Long> {
}
