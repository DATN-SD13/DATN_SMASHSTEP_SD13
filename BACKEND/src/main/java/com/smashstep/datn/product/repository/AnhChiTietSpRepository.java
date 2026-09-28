package com.smashstep.datn.product.repository;

import com.smashstep.datn.product.entity.AnhChiTietSp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnhChiTietSpRepository extends JpaRepository<AnhChiTietSp, Long> {
}
