package com.smashstep.datn.product.repository;

import com.smashstep.datn.product.entity.XuatXu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface XuatXuRepository extends JpaRepository<XuatXu, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<XuatXu> {
}
