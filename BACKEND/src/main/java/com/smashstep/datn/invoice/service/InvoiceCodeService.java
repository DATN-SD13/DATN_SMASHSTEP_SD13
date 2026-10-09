package com.smashstep.datn.invoice.service;

import com.smashstep.datn.common.exception.AppException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigInteger;
import java.util.Collection;

@Service
@RequiredArgsConstructor
public class InvoiceCodeService {
    private final EntityManager em;

    /** Call within the same transaction that inserts the invoice. All writers share the POS lock. */
    @Transactional(propagation = Propagation.MANDATORY)
    public String generateNextInvoiceCode() {
        lockInvoiceCreation();
        return nextCode(em.createQuery("select h.maHoaDon from HoaDon h", String.class).getResultList());
    }

    static String nextCode(Collection<String> codes) {
        BigInteger max = BigInteger.ZERO;
        for (String code : codes) {
            if (code != null && code.matches("^HD[0-9]+$")) max = max.max(new BigInteger(code.substring(2)));
        }
        String digits = max.add(BigInteger.ONE).toString();
        String result = "HD" + "0".repeat(Math.max(0, 6 - digits.length())) + digits;
        if (result.length() > 50) throw AppException.conflict("Mã hóa đơn đã vượt giới hạn lưu trữ");
        return result;
    }

    public void lockInvoiceCreation() {
        int result = em.unwrap(Session.class).doReturningWork(connection -> {
            if (connection.getAutoCommit()) throw new java.sql.SQLException("Invoice creation requires a managed transaction");
            try (var start = connection.prepareStatement("SELECT TOP (1) object_id FROM sys.objects"); var ignored = start.executeQuery()) { }
            try (var statement = connection.prepareStatement("DECLARE @result int; EXEC @result = sys.sp_getapplock "
                    + "@Resource=N'smashstep:pos:checkout', @LockMode='Exclusive', @LockOwner='Transaction', @LockTimeout=10000; SELECT @result")) {
                try (var rows = statement.executeQuery()) { rows.next(); return rows.getInt(1); }
            }
        });
        if (result < 0) throw AppException.conflict("Đang xử lý thanh toán khác, hãy thử lại");
    }
}
