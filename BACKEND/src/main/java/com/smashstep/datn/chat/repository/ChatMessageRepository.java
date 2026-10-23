package com.smashstep.datn.chat.repository;

import com.smashstep.datn.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
    Optional<ChatMessage> findFirstBySessionIdOrderByCreatedAtDesc(Long sessionId);
}
