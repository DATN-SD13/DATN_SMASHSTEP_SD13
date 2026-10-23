package com.smashstep.datn.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ChatDto {
    private ChatDto() {}

    public record SendMessageRequest(
            @NotBlank(message = "Nội dung tin nhắn không được để trống")
            @Size(max = 2000, message = "Tin nhắn không được vượt quá 2000 ký tự")
            String content
    ) {}

    /** Dữ liệu card sản phẩm mà AI gửi kèm tin nhắn. */
    public record ProductSuggestion(
            Long id,
            String code,
            String name,
            String brand,
            String category,
            String imageUrl,
            BigDecimal originalPrice,
            BigDecimal salePrice,
            BigDecimal discountPercent,
            boolean discounted,
            long stock,
            Long soldCount,
            List<String> availableSizes
    ) {}

    /** Metadata của tin AI được lưu JSON để F5 vẫn giữ card sản phẩm / gợi ý tiếp theo. */
    public record AiMessageMeta(
            String responseType,
            List<ProductSuggestion> products,
            List<String> suggestions
    ) {}

    public record MessageResponse(
            Long id,
            String senderType,
            Long senderId,
            String senderName,
            String content,
            String responseType,
            List<ProductSuggestion> products,
            List<String> suggestions,
            LocalDateTime createdAt
    ) {}

    public record SessionSummary(
            Long id,
            Long customerId,
            String customerCode,
            String customerName,
            String customerPhone,
            Long employeeId,
            String employeeName,
            String status,
            String lastMessage,
            LocalDateTime updatedAt,
            LocalDateTime requestedAt
    ) {}

    public record SessionDetail(
            SessionSummary session,
            List<MessageResponse> messages
    ) {}
}
