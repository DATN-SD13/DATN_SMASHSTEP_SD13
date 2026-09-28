package com.smashstep.datn.workshift.repository;

import com.smashstep.datn.workshift.entity.LichLamViecNhanVien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LichLamViecNhanVienRepository extends JpaRepository<LichLamViecNhanVien, Long> {
}
