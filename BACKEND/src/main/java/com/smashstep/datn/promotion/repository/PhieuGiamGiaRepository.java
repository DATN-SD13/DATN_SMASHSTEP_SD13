package com.smashstep.datn.promotion.repository;

import com.smashstep.datn.promotion.entity.PhieuGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface PhieuGiamGiaRepository extends JpaRepository<PhieuGiamGia, Long>,
        JpaSpecificationExecutor<PhieuGiamGia> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PhieuGiamGia p where p.id = :id")
    Optional<PhieuGiamGia> findByIdForUpdate(@Param("id") Long id);

    boolean existsByMaPhieuGiamGia(String maPhieuGiamGia);

    boolean existsByMaPhieuGiamGiaAndIdNot(
            String maPhieuGiamGia,
            Long id
    );

}
