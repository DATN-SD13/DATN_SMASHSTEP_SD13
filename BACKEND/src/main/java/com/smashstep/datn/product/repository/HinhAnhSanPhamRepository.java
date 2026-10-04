package com.smashstep.datn.product.repository;

import com.smashstep.datn.product.entity.HinhAnhSanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface HinhAnhSanPhamRepository extends JpaRepository<HinhAnhSanPham, Long> {
    boolean existsByUrlAnh(String urlAnh);
    @Query("select anh.idSanPham.id from HinhAnhSanPham anh where anh.id = :id")
    Optional<Long> laySanPhamIdTheoAnhId(@Param("id") Long id);

    List<HinhAnhSanPham> findByIdSanPham_IdOrderByIdAsc(Long sanPhamId);
    List<HinhAnhSanPham> findByIdSanPham_IdInAndIsAnhChinhTrueOrderByIdAsc(List<Long> danhSachId);
}
