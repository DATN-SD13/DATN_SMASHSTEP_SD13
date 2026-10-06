package com.smashstep.datn.product.repository;

import com.smashstep.datn.product.entity.MauSac;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MauSacRepository extends JpaRepository<MauSac, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<MauSac> {
    @Query("select thuocTinh.maMauSac from MauSac thuocTinh")
    List<String> layDanhSachMa();
}
