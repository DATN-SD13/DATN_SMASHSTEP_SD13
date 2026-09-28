package com.smashstep.datn.customer.repository;

import com.smashstep.datn.customer.entity.DiaChiKhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiaChiKhachHangRepository extends JpaRepository<DiaChiKhachHang, Long> {
}
