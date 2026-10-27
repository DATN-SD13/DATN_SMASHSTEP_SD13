package com.smashstep.datn.chat.repository;

import com.smashstep.datn.chat.entity.ChatSession;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    @EntityGraph(attributePaths = {"customer", "assignedEmployee"})
    Optional<ChatSession> findFirstByCustomerIdAndStatusNotOrderByUpdatedAtDesc(Long customerId, String status);

    @EntityGraph(attributePaths = {"customer", "assignedEmployee"})
    Optional<ChatSession> findFirstByCustomerIdOrderByUpdatedAtDesc(Long customerId);

    @EntityGraph(attributePaths = {"customer", "assignedEmployee"})
    @Query("select s from ChatSession s where s.id = :id")
    Optional<ChatSession> findDetailById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"customer", "assignedEmployee"})
    @Query("""
        select s from ChatSession s
        where s.status in ('WAITING_STAFF', 'STAFF_ACTIVE')
           or (:closed = true and s.status = 'CLOSED')
        order by case when s.status = 'WAITING_STAFF' then 0 when s.status = 'STAFF_ACTIVE' then 1 else 2 end,
                 s.updatedAt desc
    """)
    List<ChatSession> findForStaff(@Param("closed") boolean closed);
}
