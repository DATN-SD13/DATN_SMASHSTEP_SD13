package com.smashstep.datn.product.repository;

import com.smashstep.datn.product.entity.CoGiay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CoGiayRepository extends JpaRepository<CoGiay, Long> {
}
