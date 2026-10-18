package com.smashstep.datn.employee.repository;
import com.smashstep.datn.employee.entity.NhanVienImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface NhanVienImportBatchRepository extends JpaRepository<NhanVienImportBatch,String> {
    List<NhanVienImportBatch> findTop10ByCreatedByOrderByCreatedAtDesc(Long actor);
}
