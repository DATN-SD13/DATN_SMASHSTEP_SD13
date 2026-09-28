package com.smashstep.datn.workshift.repository;

import com.smashstep.datn.workshift.entity.LichLamViec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LichLamViecRepository extends JpaRepository<LichLamViec, Long> {
}
