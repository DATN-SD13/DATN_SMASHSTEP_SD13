package com.smashstep.datn.promotion.repository;

import com.smashstep.datn.promotion.entity.PhieuGiamGiaCaNhan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhieuGiamGiaCaNhanRepository extends JpaRepository<PhieuGiamGiaCaNhan, Long> {
}
