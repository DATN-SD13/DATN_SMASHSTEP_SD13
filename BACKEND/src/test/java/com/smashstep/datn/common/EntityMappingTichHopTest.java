package com.smashstep.datn.common;

import com.smashstep.datn.DatnApplication;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/** Read every entity against the real SQL Server without changing rows or schema. */
@SpringBootTest(classes = DatnApplication.class, properties = {
        "spring.jpa.open-in-view=false", "spring.jpa.hibernate.ddl-auto=none", "spring.jpa.show-sql=false" })
@Transactional(readOnly = true)
class EntityMappingTichHopTest {
    @Autowired
    private EntityManager entityManager;

    @Test
    void allEntitiesCanBeReadFromTheCurrentDatabase() {
        var entities = entityManager.getMetamodel().getEntities();
        assertFalse(entities.isEmpty());
        assertAll(entities.stream().map(entity -> () -> assertDoesNotThrow(() ->
                entityManager.createQuery("select e from " + entity.getName() + " e")
                        .setMaxResults(1).getResultList(), entity.getName())));
    }
}
