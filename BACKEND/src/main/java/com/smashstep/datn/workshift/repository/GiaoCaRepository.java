package com.smashstep.datn.workshift.repository;

import com.smashstep.datn.workshift.entity.GiaoCa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GiaoCaRepository extends JpaRepository<GiaoCa, Long> {
}
