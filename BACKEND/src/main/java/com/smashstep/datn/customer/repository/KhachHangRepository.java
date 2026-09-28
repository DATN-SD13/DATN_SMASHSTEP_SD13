package com.smashstep.datn.customer.repository;

import com.smashstep.datn.customer.entity.KhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KhachHangRepository extends JpaRepository<KhachHang, Long> {
}
