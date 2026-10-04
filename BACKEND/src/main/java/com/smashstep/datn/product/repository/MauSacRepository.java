package com.smashstep.datn.product.repository;

import com.smashstep.datn.product.entity.MauSac;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MauSacRepository extends JpaRepository<MauSac, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<MauSac> {
}
