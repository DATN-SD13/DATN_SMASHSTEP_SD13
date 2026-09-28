package com.smashstep.datn.workshift.repository;

import com.smashstep.datn.workshift.entity.CaLam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CaLamRepository extends JpaRepository<CaLam, Long> {
}
