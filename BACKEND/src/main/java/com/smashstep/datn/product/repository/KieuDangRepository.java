package com.smashstep.datn.product.repository;

import com.smashstep.datn.product.entity.KieuDang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface KieuDangRepository extends JpaRepository<KieuDang, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<KieuDang> {
    @Query("select thuocTinh.maKieuDang from KieuDang thuocTinh")
    List<String> layDanhSachMa();
}
