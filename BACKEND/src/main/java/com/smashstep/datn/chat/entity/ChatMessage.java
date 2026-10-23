package com.smashstep.datn.chat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "chat_tin_nhan")
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_phien", nullable = false)
    private ChatSession session;

    @Column(name = "loai_nguoi_gui", nullable = false, length = 20)
    private String senderType;

    @Column(name = "id_nguoi_gui")
    private Long senderId;

    @Column(name = "ten_nguoi_gui", length = 150)
    private String senderName;

    @Column(name = "noi_dung", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String content;

    @Column(name = "metadata_json", columnDefinition = "NVARCHAR(MAX)")
    private String metadataJson;

    @Column(name = "ngay_gui", nullable = false)
    private LocalDateTime createdAt;
}
