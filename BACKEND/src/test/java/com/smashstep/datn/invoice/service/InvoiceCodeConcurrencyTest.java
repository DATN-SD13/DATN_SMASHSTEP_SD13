package com.smashstep.datn.invoice.service;

import com.smashstep.datn.DatnApplication;
import com.smashstep.datn.invoice.entity.HoaDon;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes=DatnApplication.class, properties={"spring.jpa.show-sql=false","spring.jpa.open-in-view=false"})
class InvoiceCodeConcurrencyTest {
    @Autowired InvoiceCodeService codes;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;

    @Test void concurrentCommittedWritersReceiveDifferentConsecutiveCodes() throws Exception {
        String testMarker="Invoice code concurrency test "+UUID.randomUUID();
        var transaction=new TransactionTemplate(transactions);
        var firstHasLock=new CountDownLatch(1); var secondStarted=new CountDownLatch(1);
        var allowFirstCommit=new CountDownLatch(1); var secondHasCode=new CountDownLatch(1);
        var executor=Executors.newFixedThreadPool(2);
        try {
            var first=executor.submit(()->transaction.execute(status->{
                String code=codes.generateNextInvoiceCode();
                var invoice=new HoaDon(); invoice.setMaHoaDon(code); invoice.setGhiChu(testMarker); em.persist(invoice); em.flush(); firstHasLock.countDown();
                try { if(!allowFirstCommit.await(5,TimeUnit.SECONDS)) throw new IllegalStateException("Missing commit signal"); }
                catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
                return code;
            }));
            assertTrue(firstHasLock.await(10,TimeUnit.SECONDS));
            var second=executor.submit(()->transaction.execute(status->{
                secondStarted.countDown(); String code=codes.generateNextInvoiceCode(); secondHasCode.countDown();
                var invoice=new HoaDon(); invoice.setMaHoaDon(code); invoice.setGhiChu(testMarker); em.persist(invoice); em.flush(); return code;
            }));
            assertTrue(secondStarted.await(5,TimeUnit.SECONDS));
            assertFalse(secondHasCode.await(250,TimeUnit.MILLISECONDS),"Second writer must wait for the first transaction");
            allowFirstCommit.countDown(); String firstCode=first.get(10,TimeUnit.SECONDS),secondCode=second.get(10,TimeUnit.SECONDS);
            assertNotEquals(firstCode,secondCode);
            assertEquals(new java.math.BigInteger(firstCode.substring(2)).add(java.math.BigInteger.ONE),new java.math.BigInteger(secondCode.substring(2)));
        } finally {
            allowFirstCommit.countDown(); executor.shutdown(); executor.awaitTermination(15,TimeUnit.SECONDS);
            // Remove only these two freshly created test fixtures, never any existing invoices.
            transaction.executeWithoutResult(status->em.createQuery("select h from HoaDon h where h.ghiChu=:marker",HoaDon.class)
                    .setParameter("marker",testMarker).getResultList().forEach(em::remove));
        }
    }
}
