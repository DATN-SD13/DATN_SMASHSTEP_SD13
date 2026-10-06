package com.smashstep.datn.promotion.repository;

import com.smashstep.datn.promotion.entity.DotGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DotGiamGiaRepository
        extends JpaRepository<DotGiamGia, Long>,
        JpaSpecificationExecutor<DotGiamGia> {

    boolean existsByMaDotGiamGia(String maDotGiamGia);

    Optional<DotGiamGia> findByMaDotGiamGia(String maDotGiamGia);
}