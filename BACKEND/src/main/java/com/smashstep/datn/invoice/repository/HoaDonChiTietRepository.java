package com.smashstep.datn.invoice.repository;

import com.smashstep.datn.invoice.entity.HoaDonChiTiet;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HoaDonChiTietRepository extends JpaRepository<HoaDonChiTiet, Long> {
    @EntityGraph(attributePaths = {
            "idSanPhamChiTiet",
            "idSanPhamChiTiet.idSanPham",
            "idSanPhamChiTiet.idMauSac",
            "idSanPhamChiTiet.idKichThuoc"
    })
    List<HoaDonChiTiet> findByIdHoaDonIdOrderByIdAsc(Long idHoaDon);
}
