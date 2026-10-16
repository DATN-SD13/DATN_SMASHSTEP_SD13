package com.smashstep.datn.schedule.repository;

import com.smashstep.datn.schedule.entity.CaLamViec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaLamViecRepository extends JpaRepository<CaLamViec, Long> {

    List<CaLamViec> findByTrangThaiOrderByGioBatDauAsc(Integer trangThai);
    List<CaLamViec> findAllByOrderByGioBatDauAsc();

    boolean existsByMaCa(String maCa);
    boolean existsByTenCaIgnoreCase(String tenCa);
    boolean existsByTenCaIgnoreCaseAndIdNot(String tenCa, Long id);

    java.util.Optional<CaLamViec> findByMaCa(String maCa);
}
