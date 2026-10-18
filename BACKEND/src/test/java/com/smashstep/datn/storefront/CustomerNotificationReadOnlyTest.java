package com.smashstep.datn.storefront;
import com.smashstep.datn.DatnApplication;
import com.smashstep.datn.storefront.checkout.CustomerCommerceService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

/** A real read-only transaction must commit even when some vouchers fail preview eligibility. */
@SpringBootTest(classes=DatnApplication.class,properties={"spring.jpa.hibernate.ddl-auto=none","spring.sql.init.mode=never","spring.jpa.show-sql=false"})
class CustomerNotificationReadOnlyTest {
    @Autowired CustomerCommerceService shop;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager manager;
    @Test void ineligibleVouchersDoNotMarkReadOnlyNotificationFeedRollbackOnly() {
        var transaction=new TransactionTemplate(manager);transaction.setReadOnly(true);
        assertDoesNotThrow(()->transaction.execute(status->{
            assertTrue(((String)em.createNativeQuery("select DB_NAME()").getSingleResult()).contains("Audit"));
            var id=em.createQuery("select c.id from KhachHang c where c.trangThai=1 order by c.id",Long.class).setMaxResults(1).getSingleResult();
            assertNotNull(shop.notices(id));return null;
        }));
    }
}
