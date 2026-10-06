package com.smashstep.datn.customer.repository;

import com.smashstep.datn.customer.entity.DiaChiKhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiaChiKhachHangRepository
        extends JpaRepository<DiaChiKhachHang, Long> {

    List<DiaChiKhachHang>
    findByIdKhachHang_IdAndTrangThaiOrderByIsMacDinhDescIdDesc(
            Long customerId,
            Integer status
    );

    Optional<DiaChiKhachHang>
    findByIdAndIdKhachHang_Id(
            Long id,
            Long customerId
    );

    Optional<DiaChiKhachHang>
    findFirstByIdKhachHang_IdAndTrangThaiAndIsMacDinhTrue(
            Long customerId,
            Integer status
    );

    Optional<DiaChiKhachHang>
    findFirstByIdKhachHang_IdAndTrangThaiOrderByIdDesc(
            Long customerId,
            Integer status
    );
}