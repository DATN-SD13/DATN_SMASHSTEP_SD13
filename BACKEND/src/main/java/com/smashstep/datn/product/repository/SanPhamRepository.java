package com.smashstep.datn.product.repository;

import com.smashstep.datn.product.entity.SanPham;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SanPhamRepository extends JpaRepository<SanPham, Long>, JpaSpecificationExecutor<SanPham> {
    @Override
    @EntityGraph(attributePaths = {"idDanhMuc", "idThuongHieu", "idChatLieu", "idKieuDang", "idCoGiay", "idXuatXu"})
    Page<SanPham> findAll(Specification<SanPham> boLoc, Pageable phanTrang);

    boolean existsByMaSanPham(String maSanPham);
    boolean existsByMaSanPhamIgnoreCase(String maSanPham);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sanPham from SanPham sanPham where sanPham.id = :id")
    Optional<SanPham> timVaKhoaTheoId(@Param("id") Long id);
}
