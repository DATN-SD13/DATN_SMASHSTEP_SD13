package com.smashstep.datn.product.repository;

import com.smashstep.datn.product.entity.ThuongHieu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ThuongHieuRepository extends JpaRepository<ThuongHieu, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<ThuongHieu> {
    @Query("select thuocTinh.maThuongHieu from ThuongHieu thuocTinh")
    List<String> layDanhSachMa();
}
