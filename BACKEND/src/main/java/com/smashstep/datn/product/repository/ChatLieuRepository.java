package com.smashstep.datn.product.repository;

import com.smashstep.datn.product.entity.ChatLieu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatLieuRepository extends JpaRepository<ChatLieu, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<ChatLieu> {
    @Query("select thuocTinh.maChatLieu from ChatLieu thuocTinh")
    List<String> layDanhSachMa();
}
