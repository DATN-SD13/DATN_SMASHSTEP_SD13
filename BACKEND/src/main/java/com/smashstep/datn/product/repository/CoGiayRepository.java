package com.smashstep.datn.product.repository;

import com.smashstep.datn.product.entity.CoGiay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CoGiayRepository extends JpaRepository<CoGiay, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<CoGiay> {
    @Query("select thuocTinh.maCoGiay from CoGiay thuocTinh")
    List<String> layDanhSachMa();
}
