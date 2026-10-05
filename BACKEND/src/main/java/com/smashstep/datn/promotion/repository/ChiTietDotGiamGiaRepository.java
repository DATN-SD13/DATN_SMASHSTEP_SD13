package com.smashstep.datn.promotion.repository;

import com.smashstep.datn.promotion.entity.ChiTietDotGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietDotGiamGiaRepository extends JpaRepository<ChiTietDotGiamGia, Long> {

    List<ChiTietDotGiamGia> findByIdDotGiamGiaIdAndTrangThai(
            Long idDotGiamGia,
            Integer trangThai
    );
}