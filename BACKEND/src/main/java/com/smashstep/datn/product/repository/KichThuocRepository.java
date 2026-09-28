package com.smashstep.datn.product.repository;

import com.smashstep.datn.product.entity.KichThuoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KichThuocRepository extends JpaRepository<KichThuoc, Long> {
}
