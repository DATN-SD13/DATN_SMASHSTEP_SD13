package com.smashstep.datn.employee.repository;

import com.smashstep.datn.employee.entity.VaiTro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VaiTroRepository extends JpaRepository<VaiTro, Long> {

    List<VaiTro> findByTrangThaiOrderByIdAsc(Integer trangThai);
}