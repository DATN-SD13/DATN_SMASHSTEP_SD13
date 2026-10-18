package com.smashstep.datn.common.config;

import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Canonical annotations apply by default; omit only physically absent legacy columns. */
@Configuration
public class SchemaCompatibilityConfig {
    @Bean
    public EntityManagerFactoryBuilderCustomizer physicalSchemaMappings(DatabaseCapabilities database) {
        return builder -> builder.setPersistenceUnitPostProcessors(unit -> {
            unit.getMappingFileNames().remove("META-INF/orm-local-smashstep.xml");
            String[][] optional = {
                    {"hoa_don", "tien_giam_gia"}, {"hoa_don", "dia_chi_giao_hang"},
                    {"lich_su_thanh_toan", "id_phuong_thuc_thanh_toan"},
                    {"dot_giam_gia", "mo_ta"}, {"phieu_giam_gia", "hinh_thuc_phieu"}
            };
            for (String[] column : optional) {
                if (!database.hasColumn(column[0], column[1])) {
                    unit.addMappingFileName("META-INF/compat/" + column[0] + "-" + column[1] + ".xml");
                }
            }
        });
    }
}
