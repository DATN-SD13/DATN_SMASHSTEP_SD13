package com.smashstep.datn.employee.repository;

import com.smashstep.datn.employee.entity.QuyenHanChucNang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuyenHanChucNangRepository extends JpaRepository<QuyenHanChucNang, Long> {
}
