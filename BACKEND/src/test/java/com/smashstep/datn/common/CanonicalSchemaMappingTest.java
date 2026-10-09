package com.smashstep.datn.common;

import com.smashstep.datn.common.config.DatabaseCapabilities;
import com.smashstep.datn.common.config.SchemaCompatibilityConfig;
import com.smashstep.datn.promotion.entity.PhieuGiamGia;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CanonicalSchemaMappingTest {
    @Test void everyEntityColumnExistsInCanonicalSqlWithDocumentedAdditiveMigrations() throws Exception {
        String sql = Files.readString(Path.of("../Database/01_sqlSD13.sql"));
        Map<String, Set<String>> tables = new HashMap<>();
        var blocks = Pattern.compile("CREATE TABLE (\\w+) \\(([\\s\\S]*?)\\n\\);", Pattern.CASE_INSENSITIVE).matcher(sql);
        while (blocks.find()) {
            Set<String> columns = new HashSet<>();
            var rows = Pattern.compile("(?m)^\\s*(\\w+)\\s+(BIGINT|INT|BIT|VARCHAR|NVARCHAR|DATETIME2|DATE|DECIMAL)\\b", Pattern.CASE_INSENSITIVE).matcher(blocks.group(2));
            while (rows.find()) columns.add(rows.group(1));
            tables.put(blocks.group(1), columns);
        }
        assertEquals(25, tables.size());
        String migrations = Files.readString(Path.of("../Database/FULL_DB.sql"));
        assertTrue(migrations.contains("ADD [hinh_thuc_nhan] TINYINT NULL"));
        assertTrue(migrations.contains("vo_han"));
        tables.get("hoa_don").add("hinh_thuc_nhan");
        tables.get("phieu_giam_gia").add("vo_han");
        try (var sources = Files.walk(Path.of("src/main/java"))) {
            for (Path source : sources.filter(p -> p.toString().endsWith(".java")).toList()) {
                String name = Path.of("src/main/java").relativize(source).toString().replace('\\', '.').replace('/', '.').replace(".java", "");
                Class<?> type = Class.forName(name);
                if (!type.isAnnotationPresent(Entity.class)) continue;
                String table = type.getAnnotation(Table.class).name();
                assertNotNull(tables.get(table), table);
                for (var field : type.getDeclaredFields()) {
                    String column = field.isAnnotationPresent(Column.class) ? field.getAnnotation(Column.class).name()
                            : field.isAnnotationPresent(JoinColumn.class) ? field.getAnnotation(JoinColumn.class).name() : null;
                    if (column != null) assertTrue(tables.get(table).contains(column), type.getName() + "." + column);
                }
            }
        }
        assertTrue(tables.get("phieu_giam_gia").contains("vo_han"));
        assertEquals(Boolean.class, PhieuGiamGia.class.getDeclaredField("voHan").getType());
    }

    @Test void canonicalDatabaseKeepsEveryCanonicalMappingDespiteOldLocalOverride() {
        var database = mock(DatabaseCapabilities.class);
        when(database.hasColumn(anyString(), anyString())).thenReturn(true);
        var builder = mock(EntityManagerFactoryBuilder.class);
        var unit = new MutablePersistenceUnitInfo(); unit.addMappingFileName("META-INF/orm-local-smashstep.xml");
        doAnswer(call -> { for (var processor : (PersistenceUnitPostProcessor[]) call.getRawArguments()[0]) processor.postProcessPersistenceUnitInfo(unit); return null; })
                .when(builder).setPersistenceUnitPostProcessors(any(PersistenceUnitPostProcessor[].class));
        new SchemaCompatibilityConfig().physicalSchemaMappings(database).customize(builder);
        assertTrue(unit.getMappingFileNames().isEmpty());
    }
}
