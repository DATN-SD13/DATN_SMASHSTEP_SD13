package com.smashstep.datn.product.service;

import com.smashstep.datn.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.sql.Types;

@Component
@RequiredArgsConstructor
public class KhoaGhiSanPham {
    private final JdbcTemplate jdbcTemplate;

    public void khoa(String resource) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Duplicate protection requires a transaction");
        }
        // SQL Server transaction-owned lock also protects identities that have no row yet.
        // The database releases this lock only after commit/rollback, across app instances.
        Integer result = jdbcTemplate.execute((ConnectionCallback<Integer>) connection -> {
            if (connection.getAutoCommit()) throw new IllegalStateException("Expected a transaction-bound connection");
            // JDBC defers SQL Server's physical transaction until the first table access.
            // sp_getapplock itself cannot begin that transaction, so initialize it here.
            try (var begin = connection.prepareStatement("IF @@TRANCOUNT = 0 BEGIN TRANSACTION")) {
                begin.execute();
            }
            try (var statement = connection.prepareCall("{? = call sys.sp_getapplock(?, ?, ?, ?)}")) {
                statement.registerOutParameter(1, Types.INTEGER);
                statement.setString(2, "smashstep:product:" + resource);
                statement.setString(3, "Exclusive");
                statement.setString(4, "Transaction");
                statement.setInt(5, 10000);
                statement.execute();
                return statement.getInt(1);
            }
        });
        if (result == null || result < 0) {
            throw AppException.conflict("Có thao tác khác đang lưu dữ liệu. Vui lòng thử lại.");
        }
    }
}
