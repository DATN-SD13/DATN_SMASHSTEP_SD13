package com.smashstep.datn.employee.repository;

import com.smashstep.datn.employee.entity.ChucNang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChucNangRepository extends JpaRepository<ChucNang, Long> {
}
