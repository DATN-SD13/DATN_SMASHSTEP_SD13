package com.smashstep.datn.product.repository;

import com.smashstep.datn.product.entity.ChatLieu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatLieuRepository extends JpaRepository<ChatLieu, Long> {
}
