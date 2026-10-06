package com.smashstep.datn.promotion.repository;

import com.smashstep.datn.promotion.entity.PhieuGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PhieuGiamGiaRepository extends JpaRepository<PhieuGiamGia, Long>,
        JpaSpecificationExecutor<PhieuGiamGia> {
    boolean existsByMaPhieuGiamGia(String maPhieuGiamGia);

    boolean existsByMaPhieuGiamGiaAndIdNot(
            String maPhieuGiamGia,
            Long id
    );

}
