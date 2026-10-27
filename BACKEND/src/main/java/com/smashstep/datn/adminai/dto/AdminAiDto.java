package com.smashstep.datn.adminai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class AdminAiDto {
    private AdminAiDto() {}

    public record AskRequest(
            @NotBlank(message = "Câu hỏi không được để trống")
            @Size(max = 500, message = "Câu hỏi tối đa 500 ký tự")
            String message
    ) {}

    public record MetricCard(
            String key,
            String label,
            String value,
            String helper,
            String tone
    ) {}

    public record ProductInsight(
            Long id,
            String code,
            String name,
            String imageUrl,
            long sold,
            BigDecimal revenue,
            long stock,
            BigDecimal growthPercent,
            String note
    ) {}

    public record EmployeeInsight(
            Long id,
            String code,
            String name,
            long orders,
            BigDecimal revenue,
            long productsSold
    ) {}

    public record OrderStatusInsight(
            Integer status,
            String label,
            long count
    ) {}

    public record Reply(
            String content,
            String responseType,
            LocalDateTime generatedAt,
            List<MetricCard> metrics,
            List<ProductInsight> products,
            List<EmployeeInsight> employees,
            List<OrderStatusInsight> orderStatuses,
            List<String> suggestions,
            boolean reviewAvailable
    ) {}
}
