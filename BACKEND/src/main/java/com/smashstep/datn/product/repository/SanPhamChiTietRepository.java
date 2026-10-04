package com.smashstep.datn.product.repository;

import com.smashstep.datn.product.entity.SanPhamChiTiet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SanPhamChiTietRepository
        extends JpaRepository<SanPhamChiTiet, Long>, JpaSpecificationExecutor<SanPhamChiTiet> {
    @Override
    @EntityGraph(attributePaths = {"idSanPham", "idMauSac", "idKichThuoc"})
    Page<SanPhamChiTiet> findAll(Specification<SanPhamChiTiet> boLoc, Pageable phanTrang);

    boolean existsByMaChiTietSanPham(String maChiTietSanPham);
    boolean existsBySku(String sku);
    boolean existsByMaChiTietSanPhamIgnoreCaseAndIdNot(String maChiTietSanPham, Long id);
    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);
    boolean existsByIdSanPham_IdAndIdMauSac_IdAndIdKichThuoc_Id(
            Long sanPhamId, Long mauSacId, Long kichThuocId);
    boolean existsByIdSanPham_IdAndIdMauSac_IdAndIdKichThuoc_IdAndIdNot(
            Long sanPhamId, Long mauSacId, Long kichThuocId, Long id);

    @EntityGraph(attributePaths = {"idSanPham", "idMauSac", "idKichThuoc"})
    List<SanPhamChiTiet> findByIdSanPham_IdOrderByIdAsc(Long sanPhamId);

    @Query("""
        select chiTiet.idSanPham.id as sanPhamId, sum(chiTiet.soLuong) as tongSoLuong,
               min(chiTiet.giaBan) as giaThapNhat, max(chiTiet.giaBan) as giaCaoNhat,
               count(chiTiet.id) as soBienThe,
               count(distinct chiTiet.idMauSac.id) as soMau,
               count(distinct chiTiet.idKichThuoc.id) as soKichThuoc
        from SanPhamChiTiet chiTiet
        where chiTiet.idSanPham.id in :danhSachId
        group by chiTiet.idSanPham.id
        """)
    List<TongHopSanPham> tongHopTheoSanPham(@Param("danhSachId") List<Long> danhSachId);
}
