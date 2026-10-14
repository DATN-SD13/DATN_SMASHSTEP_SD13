package com.smashstep.datn.sales.service;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.payment.entity.PhuongThucThanhToan;
import com.smashstep.datn.sales.dto.SalesRequest;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.*;

/** Amount is money retained by the store; tendered cash is separate and may include change. */
public record PaymentPlan(List<Part> parts, BigDecimal tendered, BigDecimal change) {
    public record Part(PhuongThucThanhToan method, BigDecimal amount) {}
    public static PaymentPlan resolve(EntityManager em, SalesRequest r, BigDecimal total) {
        if (r.getPayments() == null || r.getPayments().isEmpty() || r.getPayments().size() > 2)
            throw AppException.badRequest("Cần 1 hoặc 2 khoản thanh toán");
        Set<Long> ids = new HashSet<>(); List<Part> parts = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO, cash = BigDecimal.ZERO, bank = BigDecimal.ZERO;
        for (var input : r.getPayments()) {
            if (input == null || input.getMethodId() == null || !ids.add(input.getMethodId()))
                throw AppException.badRequest("Phương thức thanh toán bị thiếu hoặc trùng");
            var method = em.find(PhuongThucThanhToan.class, input.getMethodId());
            if (method == null || !Integer.valueOf(1).equals(method.getTrangThai())
                    || !Set.of("TIEN_MAT", "CHUYEN_KHOAN").contains(method.getMaPhuongThuc()))
                throw AppException.badRequest("Chỉ hỗ trợ tiền mặt và chuyển khoản đang hoạt động");
            var amount = input.getAmount();
            if (amount == null || amount.signum() < 0 || amount.scale() > 2
                    || amount.precision() - amount.scale() > 16 || (total.signum() > 0 && amount.signum() == 0))
                throw AppException.badRequest("Số tiền mỗi khoản phải dương (trừ hóa đơn có tổng bằng 0)");
            if ("TIEN_MAT".equals(method.getMaPhuongThuc())) cash = cash.add(amount);
            else bank = bank.add(amount);
            sum = sum.add(amount); parts.add(new Part(method, amount));
        }
        if (sum.compareTo(total) != 0) throw AppException.badRequest("Tổng tiền thực thu phải bằng đúng số tiền cần trả");
        boolean hasCash = parts.stream().anyMatch(p -> "TIEN_MAT".equals(p.method().getMaPhuongThuc()));
        boolean hasBank = parts.stream().anyMatch(p -> "CHUYEN_KHOAN".equals(p.method().getMaPhuongThuc()));
        if (hasBank && bank.signum() == 0) throw AppException.badRequest("Đơn 0 đồng cần ghi nhận bằng phương thức tiền mặt, không tạo chuyển khoản 0 đồng");
        if (hasBank && !Boolean.TRUE.equals(r.getTransferConfirmed()))
            throw AppException.badRequest("Nhân viên phải xác nhận đã kiểm tra chuyển khoản thực tế");
        BigDecimal tender = r.getPaidAmount();
        if (tender == null || tender.signum() < 0 || tender.scale() > 2 || tender.precision() - tender.scale() > 16)
            throw AppException.badRequest("Cần nhập số tiền mặt khách đưa hợp lệ (0 nếu chỉ chuyển khoản)");
        if ((!hasCash && tender.signum() != 0) || tender.compareTo(cash) < 0)
            throw AppException.badRequest("Tiền mặt khách đưa chưa đủ hoặc không phù hợp phương thức");
        return new PaymentPlan(List.copyOf(parts), tender.add(bank), tender.subtract(cash));
    }
}
