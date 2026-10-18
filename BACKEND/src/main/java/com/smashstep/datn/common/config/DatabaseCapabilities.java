package com.smashstep.datn.common.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;

/** Read-only physical metadata. Never creates or migrates database objects. */
@Component
public class DatabaseCapabilities {
    private final Set<String> columns;

    public DatabaseCapabilities(JdbcTemplate jdbc) {
        columns = new HashSet<>(jdbc.query("select TABLE_NAME, COLUMN_NAME from INFORMATION_SCHEMA.COLUMNS where TABLE_SCHEMA='dbo'",
                (row, index) -> key(row.getString(1), row.getString(2))));
    }
    public boolean hasColumn(String table, String column) {
        return columns.contains(key(table, column));
    }
    private static String key(String table, String column) {
        return (table + "." + column).toLowerCase(Locale.ROOT);
    }
}
