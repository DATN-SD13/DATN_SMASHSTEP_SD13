package com.smashstep.datn.product.repository;

import com.smashstep.datn.product.entity.XuatXu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface XuatXuRepository extends JpaRepository<XuatXu, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<XuatXu> {
    @Query("select thuocTinh.maXuatXu from XuatXu thuocTinh")
    List<String> layDanhSachMa();
}
