
package com.smashstep.datn.promotion.repository;

import com.smashstep.datn.promotion.entity.ChiTietDotGiamGia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    // ==========================================
    // KIỂM TRA XUNG ĐỘT GIỮA CÁC ĐỢT GIẢM GIÁ
    // Giữ nguyên: cần kiểm tra cả đợt trong tương lai
    // ==========================================

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

    // ==========================================
    // LẤY ĐỢT GIẢM GIÁ ĐANG CÓ HIỆU LỰC
    // Phục vụ việc tính giá bán sau giảm
    // ==========================================

    @Query("""
        SELECT ct
        FROM ChiTietDotGiamGia ct
        JOIN FETCH ct.idDotGiamGia dgg
        WHERE ct.idSanPhamChiTiet.id = :productDetailId
          AND ct.trangThai = 1
          AND dgg.trangThai = 1
          AND dgg.ngayBatDau <= :now
          AND dgg.ngayKetThuc >= :now
    """)
    List<ChiTietDotGiamGia> findCurrentlyEffectiveByProductDetailId(
            @Param("productDetailId") Long productDetailId,
            @Param("now") LocalDateTime now
    );

    // ==========================================
    // XÓA CHI TIẾT KHI CẬP NHẬT DANH SÁCH
    // SẢN PHẨM THUỘC ĐỢT GIẢM GIÁ
    // ==========================================

    @Modifying
    @Query("""
        DELETE FROM ChiTietDotGiamGia ct
        WHERE ct.idDotGiamGia.id = :idDotGiamGia
    """)
    void deleteByDotGiamGiaId(
            @Param("idDotGiamGia") Long idDotGiamGia
    );
}
