package com.smashstep.datn.product.repository;

import com.smashstep.datn.product.entity.DanhMuc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DanhMucRepository extends JpaRepository<DanhMuc, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<DanhMuc> {
    @Query("select thuocTinh.maDanhMuc from DanhMuc thuocTinh")
    List<String> layDanhSachMa();
}
