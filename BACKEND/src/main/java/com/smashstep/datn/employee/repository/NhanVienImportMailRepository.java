package com.smashstep.datn.employee.repository;
import com.smashstep.datn.employee.entity.NhanVienImportMail;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface NhanVienImportMailRepository extends JpaRepository<NhanVienImportMail,Long> {
    @Query("select m from NhanVienImportMail m join fetch m.employee e left join fetch e.idVaiTro where m.batch.id=:batch order by m.rowNumber")
    List<NhanVienImportMail> findBatch(String batch);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select m from NhanVienImportMail m where m.id=:id")
    Optional<NhanVienImportMail> lockById(Long id);
}
