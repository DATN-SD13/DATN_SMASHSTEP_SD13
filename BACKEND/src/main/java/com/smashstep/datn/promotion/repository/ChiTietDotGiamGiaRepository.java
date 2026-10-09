package com.smashstep.datn.promotion.repository;

import com.smashstep.datn.promotion.entity.ChiTietDotGiamGia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietDotGiamGiaRepository
        extends JpaRepository<ChiTietDotGiamGia, Long> {

    // Lấy danh sách chi tiết theo đợt giảm giá
    List<ChiTietDotGiamGia> findByIdDotGiamGia_Id(
            Long idDotGiamGia
    );

    // Kiểm tra biến thể đã tồn tại trong đợt giảm giá chưa
    boolean existsByIdDotGiamGia_IdAndIdSanPhamChiTiet_Id(
            Long idDotGiamGia,
            Long idSanPhamChiTiet
    );


    // =====================================================
    // TÌM CÁC ĐỢT GIẢM GIÁ ĐANG HOẠT ĐỘNG
    // CỦA MỘT BIẾN THỂ SẢN PHẨM
    // =====================================================

    @Query("""
        SELECT ct
        FROM ChiTietDotGiamGia ct
        JOIN FETCH ct.idDotGiamGia dgg
        WHERE ct.idSanPhamChiTiet.id = :productDetailId
          AND ct.trangThai = 1
          AND dgg.trangThai = 1
    """)
    List<ChiTietDotGiamGia> findActiveByProductDetailId(
            @Param("productDetailId") Long productDetailId
    );


    // =====================================================
    // XÓA CHI TIẾT THEO ĐỢT GIẢM GIÁ
    // Dùng khi cập nhật lại danh sách sản phẩm
    // =====================================================

    @Modifying
    @Query("""
        DELETE FROM ChiTietDotGiamGia ct
        WHERE ct.idDotGiamGia.id = :idDotGiamGia
    """)
    void deleteByDotGiamGiaId(
            @Param("idDotGiamGia") Long idDotGiamGia
    );
    List<ChiTietDotGiamGia> findByIdDotGiamGiaIdAndTrangThai(Long idDotGiamGia, Integer trangThai);
}
